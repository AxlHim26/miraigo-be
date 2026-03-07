package com.example.japanweb.service.impl;

import com.example.japanweb.dto.request.jlpt.JlptImportRequest;
import com.example.japanweb.dto.response.jlpt.JlptImportResultDTO;
import com.example.japanweb.entity.JlptExam;
import com.example.japanweb.entity.JlptExamAsset;
import com.example.japanweb.repository.JlptExamAssetRepository;
import com.example.japanweb.repository.JlptExamRepository;
import com.example.japanweb.service.JlptImportService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
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
