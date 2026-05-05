package com.example.japanweb.service.impl;

import com.example.japanweb.dto.request.jlpt.JlptImportRequest;
import com.example.japanweb.dto.request.jlpt.JlptParsedExamImportRequest;
import com.example.japanweb.dto.request.jlpt.JlptParsedExamImportRequest.ParsedProblem;
import com.example.japanweb.dto.request.jlpt.JlptParsedExamImportRequest.ParsedQuestion;
import com.example.japanweb.dto.request.jlpt.JlptParsedExamImportRequest.ParsedSection;
import com.example.japanweb.dto.response.jlpt.JlptImportResultDTO;
import com.example.japanweb.dto.response.jlpt.JlptParsedExamImportResultDTO;
import com.example.japanweb.entity.*;
import com.example.japanweb.repository.*;
import com.example.japanweb.service.JlptImportService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JlptImportServiceImpl implements JlptImportService {

    private static final Set<String> ALLOWED_QUALITY = Set.of("usable", "very_low_text", "low_text_ratio");

    private final ObjectMapper objectMapper;
    private final JlptExamRepository jlptExamRepository;
    private final JlptExamAssetRepository jlptExamAssetRepository;
    private final JlptSectionRepository jlptSectionRepository;
    private final JlptQuestionRepository jlptQuestionRepository;
    private final JlptQuestionOptionRepository jlptQuestionOptionRepository;
    private final JlptAnswerKeyRepository jlptAnswerKeyRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public JlptImportResultDTO importFromManifest(JlptImportRequest request) {
        ManifestPayload payload = readManifest(Path.of(request.getManifestPath()));

        List<ManifestFile> files = payload.files() == null ? List.of() : payload.files();
        Map<ExamKey, List<ManifestFile>> grouped = groupFiles(files);

        if (grouped.isEmpty()) {
            return JlptImportResultDTO.builder()
                    .totalFiles(files.size())
                    .importedExams(0)
                    .importedAssets(0)
                    .warnings(List.of("Manifest does not contain valid exam records"))
                    .build();
        }

        List<String> warnings = new ArrayList<>();
        int importedExams = 0;
        int importedAssets = 0;

        for (Map.Entry<ExamKey, List<ManifestFile>> entry : grouped.entrySet()) {
            ExamKey key = entry.getKey();
            List<ManifestFile> examFiles = entry.getValue();

            String code = buildCode(key.level(), key.year(), key.month());
            JlptExam exam = jlptExamRepository.findByCode(code).orElse(null);

            if (exam == null) {
                exam = JlptExam.builder()
                        .code(code)
                        .title("JLPT " + key.level() + " " + key.year() + "." + String.format("%02d", key.month()))
                        .level(key.level())
                        .examYear(key.year())
                        .examMonth(key.month())
                        .totalDurationMinutes(defaultDurationByLevel(key.level()))
                        .published(true)
                        .build();
                exam = jlptExamRepository.save(exam);
                importedExams++;
            }

            List<JlptExamAsset> existingAssets = jlptExamAssetRepository.findByExamId(exam.getId());
            Set<String> existingKeys = new HashSet<>();
            for (JlptExamAsset asset : existingAssets) {
                existingKeys.add(asset.getAssetType() + "|" + asset.getSourcePath());
            }

            for (ManifestFile manifestFile : examFiles) {
                String quality = manifestFile.text() == null ? null : manifestFile.text().quality();
                if (quality != null && !ALLOWED_QUALITY.contains(quality)) {
                    warnings.add("Unknown quality: " + manifestFile.sourcePath());
                    continue;
                }

                String assetType = resolveAssetType(manifestFile.sourcePath());
                String dedupeKey = assetType + "|" + manifestFile.sourcePath();
                if (existingKeys.contains(dedupeKey)) {
                    continue;
                }

                JlptExamAsset asset = JlptExamAsset.builder()
                        .exam(exam)
                        .assetType(assetType)
                        .sourcePath(manifestFile.sourcePath())
                        .extractedTextPath(manifestFile.textOutputPath())
                        .quality(quality)
                        .build();
                jlptExamAssetRepository.save(asset);
                existingKeys.add(dedupeKey);
                importedAssets++;
            }
        }

        return JlptImportResultDTO.builder()
                .totalFiles(files.size())
                .importedExams(importedExams)
                .importedAssets(importedAssets)
                .warnings(warnings)
                .build();
    }

    @Override
    @Transactional
    public JlptParsedExamImportResultDTO importParsedExam(JlptParsedExamImportRequest request) {
        String examCode = request.getExamCode();
        if (examCode == null || examCode.isBlank()) {
            throw new IllegalArgumentException("examCode is required");
        }

        JlptExam exam = jlptExamRepository.findByCode(examCode)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examCode));

        List<String> warnings = new ArrayList<>();

        // If replacing, delete existing sections (cascades to questions, options, answer keys)
        if (request.isReplaceExisting()) {
            jlptSectionRepository.deleteByExamId(exam.getId());
            entityManager.flush(); // ensure delete is sent to DB before inserts
        } else {
            List<JlptSection> existing = jlptSectionRepository.findByExamIdOrderBySectionOrderAsc(exam.getId());
            if (!existing.isEmpty()) {
                throw new IllegalStateException("Exam already has sections. Use replaceExisting=true to overwrite.");
            }
        }

        int importedSections = 0;
        int importedQuestions = 0;
        int skippedQuestions = 0;
        int sectionOrder = 1;

        record SectionData(JlptSectionType type, String title, int duration, List<ParsedProblem> problems) {}
        List<SectionData> sDatas = new ArrayList<>();

        for (ParsedSection parsedSection : request.getSections()) {
            String parsedType = parsedSection.getType();
            List<ParsedProblem> problems = parsedSection.getProblems() == null ? List.of() : parsedSection.getProblems();
            
            if ("GRAMMAR_READING".equals(parsedType)) {
                int split = Math.min(3, problems.size());
                int gDur = (exam.getLevel().equals("N4") || exam.getLevel().equals("N5")) ? 20 : 25;
                int rDur = (exam.getLevel().equals("N4") || exam.getLevel().equals("N5")) ? 30 : 35;
                
                sDatas.add(new SectionData(JlptSectionType.GRAMMAR_KNOWLEDGE, "言語知識（文法）", gDur, problems.subList(0, split)));
                sDatas.add(new SectionData(JlptSectionType.READING_COMPREHENSION, "読解", rDur, problems.subList(split, problems.size())));
            } else {
                sDatas.add(new SectionData(
                    mapSectionType(parsedType),
                    sectionTitle(parsedType),
                    sectionDuration(parsedType, exam.getLevel()),
                    problems
                ));
            }
        }

        // Track a global question number offset so VOCABULARY and GRAMMAR_READING
        // go into separate sections but question numbers stay unique per section.
        for (SectionData sd : sDatas) {
            JlptSectionType sectionType = sd.type();
            String sectionTitle = sd.title();
            int durationMinutes = sd.duration();

            // Skip LISTENING — no audio data available
            if (sectionType == JlptSectionType.LISTENING) {
                warnings.add("LISTENING section skipped — no audio data available for import");
                continue;
            }

            // Flatten all problems into questions for this section
            List<ParsedQuestion> allQuestions = new ArrayList<>();
            if (sd.problems() != null) {
                for (ParsedProblem problem : sd.problems()) {
                    if (problem.getQuestions() != null) {
                        allQuestions.addAll(problem.getQuestions());
                    }
                }
            }

            // Skip sections with no valid questions
            List<ParsedQuestion> validQuestions = allQuestions.stream()
                    .filter(q -> isValidQuestion(q, warnings))
                    .toList();

            skippedQuestions += (allQuestions.size() - validQuestions.size());

            if (validQuestions.isEmpty()) {
                warnings.add(sectionType.name() + " section skipped — no valid questions");
                continue;
            }

            JlptSection section = jlptSectionRepository.save(
                    JlptSection.builder()
                            .exam(exam)
                            .sectionType(sectionType)
                            .title(sectionTitle)
                            .sectionOrder(sectionOrder++)
                            .durationMinutes(durationMinutes)
                            .build()
            );
            importedSections++;

            for (ParsedQuestion pq : validQuestions) {
                int questionNumber = parseQuestionNumber(pq.getNumber());

                JlptQuestion question = jlptQuestionRepository.save(
                        JlptQuestion.builder()
                                .section(section)
                                .questionNumber(questionNumber)
                                .prompt(pq.getPrompt().trim())
                                .explanation(pq.getExplanation() != null ? pq.getExplanation().trim() : null)
                                .build()
                );

                int optionOrder = 1;
                for (JlptParsedExamImportRequest.ParsedOption option : pq.getOptions()) {
                    jlptQuestionOptionRepository.save(
                            JlptQuestionOption.builder()
                                    .question(question)
                                    .optionKey(option.getKey())
                                    .optionText(option.getText().trim())
                                    .optionOrder(optionOrder++)
                                    .build()
                    );
                }

                jlptAnswerKeyRepository.save(
                        JlptAnswerKey.builder()
                                .question(question)
                                .correctOptionKey(pq.getCorrectAnswer())
                                .scoreWeight(1)
                                .build()
                );

                importedQuestions++;
            }
        }

        // Mark exam as COMPLETE
        exam.setContentStatus(JlptContentStatus.COMPLETE);
        jlptExamRepository.save(exam);

        return JlptParsedExamImportResultDTO.builder()
                .examCode(examCode)
                .importedSections(importedSections)
                .importedQuestions(importedQuestions)
                .skippedQuestions(skippedQuestions)
                .warnings(warnings)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean isValidQuestion(ParsedQuestion q, List<String> warnings) {
        if (q.getNumber() == null || q.getPrompt() == null || q.getPrompt().isBlank()) {
            warnings.add("Skipped question with missing number or prompt");
            return false;
        }
        if (q.getOptions() == null || q.getOptions().size() < 2) {
            warnings.add("Skipped Q" + q.getNumber() + ": fewer than 2 options");
            return false;
        }
        if (q.getCorrectAnswer() == null || q.getCorrectAnswer().isBlank()) {
            warnings.add("Skipped Q" + q.getNumber() + ": missing correctAnswer");
            return false;
        }
        // Ensure at least one option key matches the correct answer
        boolean hasMatchingOption = q.getOptions().stream()
                .anyMatch(o -> o.getKey() != null && o.getKey().equals(q.getCorrectAnswer()));
        if (!hasMatchingOption) {
            warnings.add("Skipped Q" + q.getNumber() + ": correctAnswer '" + q.getCorrectAnswer()
                    + "' does not match any option key");
            return false;
        }
        // Deduplicate option keys — keep only unique keys (first occurrence wins)
        Set<String> seenKeys = new LinkedHashSet<>();
        for (var opt : q.getOptions()) {
            if (opt.getKey() == null || !seenKeys.add(opt.getKey())) {
                warnings.add("Q" + q.getNumber() + ": duplicate option key '" + opt.getKey() + "' — extras ignored");
            }
        }
        return true;
    }

    private int parseQuestionNumber(String number) {
        try {
            return Integer.parseInt(number.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private JlptSectionType mapSectionType(String parsedType) {
        return switch (parsedType) {
            case "VOCABULARY" -> JlptSectionType.LANGUAGE_KNOWLEDGE;
            case "GRAMMAR_READING" -> JlptSectionType.READING;
            case "LISTENING" -> JlptSectionType.LISTENING;
            default -> throw new IllegalArgumentException("Unknown section type: " + parsedType);
        };
    }

    private String sectionTitle(String parsedType) {
        return switch (parsedType) {
            case "VOCABULARY" -> "言語知識（文字・語彙）";
            case "GRAMMAR_READING" -> "言語知識（文法）・読解";
            case "LISTENING" -> "聴解";
            default -> parsedType;
        };
    }

    private int sectionDuration(String parsedType, String level) {
        return switch (parsedType) {
            case "VOCABULARY" -> 25;
            case "GRAMMAR_READING" -> level.equals("N4") || level.equals("N5") ? 50 : 60;
            case "LISTENING" -> level.equals("N4") || level.equals("N5") ? 35 : 40;
            default -> 30;
        };
    }

    // ── manifest import helpers ───────────────────────────────────────────────

    private Map<ExamKey, List<ManifestFile>> groupFiles(List<ManifestFile> files) {
        Map<ExamKey, List<ManifestFile>> grouped = new LinkedHashMap<>();
        for (ManifestFile file : files) {
            if (file == null || file.sourcePath() == null || file.level() == null || file.year() == null) {
                continue;
            }
            int month = inferMonth(file.sourcePath());
            ExamKey key = new ExamKey(file.level(), file.year(), month);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(file);
        }
        return grouped;
    }

    private ManifestPayload readManifest(Path manifestPath) {
        if (!Files.exists(manifestPath)) {
            throw new IllegalArgumentException("Manifest not found: " + manifestPath);
        }

        try {
            return objectMapper.readValue(manifestPath.toFile(), ManifestPayload.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot read manifest: " + exception.getMessage(), exception);
        }
    }

    private String buildCode(String level, int year, int month) {
        return level + "-" + year + "-" + String.format("%02d", month);
    }

    private int inferMonth(String sourcePath) {
        String normalized = sourcePath.toLowerCase(Locale.ROOT);
        if (normalized.contains("7月") || normalized.contains("t7") || normalized.contains(".07")) {
            return 7;
        }
        if (normalized.contains("12月") || normalized.contains("t12") || normalized.contains(".12")) {
            return 12;
        }
        return 12;
    }

    private int defaultDurationByLevel(String level) {
        return switch (level) {
            case "N1" -> 170;
            case "N2" -> 155;
            case "N3" -> 140;
            case "N4" -> 125;
            default -> 110;
        };
    }

    private String resolveAssetType(String sourcePath) {
        String lower = sourcePath.toLowerCase(Locale.ROOT);
        if (lower.contains("answer") || lower.contains("đáp án") || lower.contains("答案")) {
            return "ANSWER";
        }
        if (lower.contains("listening") || lower.contains("script") || lower.contains("听力") || lower.contains("nghe")) {
            return "LISTENING_SCRIPT";
        }
        return "QUESTION";
    }

    private record ExamKey(String level, Integer year, Integer month) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ManifestPayload(List<ManifestFile> files) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ManifestFile(String sourcePath, String level, Integer year, String textOutputPath, TextMeta text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TextMeta(String quality) {
    }
}
