package com.example.japanweb.service.impl;

import com.example.japanweb.dto.request.jlpt.JlptSaveAnswersRequest;
import com.example.japanweb.dto.response.jlpt.*;
import com.example.japanweb.entity.*;
import com.example.japanweb.exception.ApiException;
import com.example.japanweb.exception.ErrorCode;
import com.example.japanweb.repository.*;
import com.example.japanweb.service.JlptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JlptServiceImpl implements JlptService {

    private static final int MAX_SECTION_SCALE = 60;

    /**
     * Per-level pass thresholds based on the official JLPT scoring rubric.
     * All levels use a 3-section model scaled to 60 points each (180 max).
     *
     * <ul>
     *   <li>N1/N2: total ≥ 100, each section ≥ 19</li>
     *   <li>N3:    total ≥ 95,  each section ≥ 19</li>
     *   <li>N4/N5: total ≥ 90,  each section ≥ 38</li>
     * </ul>
     *
     * Defaults to N4/N5 thresholds for any unrecognised level.
     */
    private static final Map<String, int[]> LEVEL_PASS_THRESHOLDS = Map.of(
            "N1", new int[]{100, 19},
            "N2", new int[]{90,  19},
            "N3", new int[]{95,  19},
            "N4", new int[]{90,  38},
            "N5", new int[]{80,  38}
    );
    private static final int[] DEFAULT_PASS_THRESHOLDS = {90, 38};

    private final JlptExamRepository jlptExamRepository;
    private final JlptSectionRepository jlptSectionRepository;
    private final JlptQuestionRepository jlptQuestionRepository;
    private final JlptQuestionOptionRepository jlptQuestionOptionRepository;
    private final JlptAnswerKeyRepository jlptAnswerKeyRepository;
    private final JlptAttemptRepository jlptAttemptRepository;
    private final JlptAttemptAnswerRepository jlptAttemptAnswerRepository;
    private final JlptExamAssetRepository jlptExamAssetRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JlptExamListItemDTO> getPublishedExams() {
        return jlptExamRepository.findByPublishedTrueAndContentStatusOrderByExamYearDescExamMonthDescLevelAsc(JlptContentStatus.COMPLETE).stream()
                .map(exam -> JlptExamListItemDTO.builder()
                        .id(exam.getId())
                        .code(exam.getCode())
                        .title(exam.getTitle())
                        .level(exam.getLevel())
                        .examYear(exam.getExamYear())
                        .examMonth(exam.getExamMonth())
                        .totalDurationMinutes(exam.getTotalDurationMinutes())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JlptExamDetailDTO getExamDetail(Long examId) {
        JlptExam exam = getExamOrThrow(examId);
        List<JlptSection> sections = jlptSectionRepository.findByExamIdOrderBySectionOrderAsc(examId);
        List<JlptSectionDTO> sectionDTOs = toSectionDTOs(sections);
        List<JlptExamAssetDTO> assets = jlptExamAssetRepository.findByExamId(examId).stream()
                .map(asset -> JlptExamAssetDTO.builder()
                        .id(asset.getId())
                        .assetType(asset.getAssetType())
                        .sourcePath(asset.getSourcePath())
                        .extractedTextPath(asset.getExtractedTextPath())
                        .quality(asset.getQuality())
                        .build())
                .toList();

        return JlptExamDetailDTO.builder()
                .id(exam.getId())
                .code(exam.getCode())
                .title(exam.getTitle())
                .level(exam.getLevel())
                .examYear(exam.getExamYear())
                .examMonth(exam.getExamMonth())
                .totalDurationMinutes(exam.getTotalDurationMinutes())
                .assets(assets)
                .sections(sectionDTOs)
                .build();
    }

    @Override
    @Transactional
    public JlptStartAttemptResponseDTO startAttempt(Long examId, Long userId) {
        JlptExam exam = getExamOrThrow(examId);

        Optional<JlptAttempt> existingAttempt = jlptAttemptRepository
                .findTopByExamIdAndUserIdAndStatusOrderByCreatedAtDesc(examId, userId, JlptAttemptStatus.IN_PROGRESS);

        if (existingAttempt.isPresent()) {
            if (getRemainingSeconds(existingAttempt.get()) <= 0) {
                autoSubmitExpiredAttempt(existingAttempt.get());
                existingAttempt = Optional.empty();
            } else {
                return buildAttemptSessionResponse(existingAttempt.get());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        // Add 5 minutes grace period for network latency
        LocalDateTime expiresAt = now.plusMinutes(exam.getTotalDurationMinutes()).plusMinutes(5);

        JlptAttempt attempt = jlptAttemptRepository.save(
                JlptAttempt.builder()
                        .exam(exam)
                        .user(userRepository.getReferenceById(userId))
                        .status(JlptAttemptStatus.IN_PROGRESS)
                        .startedAt(now)
                        .expiresAt(expiresAt)
                        .build()
        );

        return buildAttemptSessionResponse(attempt);
    }

    @Override
    @Transactional
    public JlptStartAttemptResponseDTO getAttemptSession(Long attemptId, Long userId) {
        JlptAttempt attempt = getAttemptOrThrow(attemptId, userId);
        if (attempt.getStatus() == JlptAttemptStatus.IN_PROGRESS && getRemainingSeconds(attempt) <= 0) {
            autoSubmitExpiredAttempt(attempt);
            attempt = getAttemptOrThrow(attemptId, userId);
        }
        return buildAttemptSessionResponse(attempt);
    }

    private JlptStartAttemptResponseDTO buildAttemptSessionResponse(JlptAttempt attempt) {
        int remainingSeconds = getRemainingSeconds(attempt);

        List<JlptAttemptAnswerDTO> answers = jlptAttemptAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .map(answer -> JlptAttemptAnswerDTO.builder()
                        .questionId(answer.getQuestion().getId())
                        .selectedOptionKey(answer.getSelectedOptionKey())
                        .build())
                .toList();

        return JlptStartAttemptResponseDTO.builder()
                .attemptId(attempt.getId())
                .examId(attempt.getExam().getId())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .totalDurationMinutes(attempt.getExam().getTotalDurationMinutes())
                .remainingSeconds(remainingSeconds)
                .answers(answers)
                .build();
    }

    @Override
    @Transactional
    public JlptSaveAnswersResponseDTO saveAnswers(Long attemptId, Long userId, JlptSaveAnswersRequest request) {
        JlptAttempt attempt = getAttemptOrThrow(attemptId, userId);
        ensureAttemptInProgress(attempt);
        ensureAttemptNotExpired(attempt);

        List<JlptSection> sections = jlptSectionRepository.findByExamIdOrderBySectionOrderAsc(attempt.getExam().getId());
        Set<Long> validQuestionIds = getQuestionIdsBySections(sections);
        Map<Long, Set<String>> validOptionKeysByQuestionId = getValidOptionKeysByQuestionId(validQuestionIds);

        int savedCount = 0;
        for (JlptSaveAnswersRequest.AnswerItem item : request.getAnswers()) {
            Long questionId = item.getQuestionId();
            if (!validQuestionIds.contains(questionId)) {
                throw new ApiException(ErrorCode.JLPT_QUESTION_NOT_IN_EXAM);
            }

            String normalizedOptionKey = normalizeOptionKey(item.getSelectedOptionKey());
            if (normalizedOptionKey != null) {
                Set<String> validOptionKeys = validOptionKeysByQuestionId.getOrDefault(questionId, Set.of());
                if (!validOptionKeys.contains(normalizedOptionKey)) {
                    throw new ApiException(ErrorCode.JLPT_OPTION_KEY_INVALID);
                }
            }

            // Single INSERT … ON CONFLICT DO UPDATE per answer instead of a
            // SELECT + conditional INSERT/UPDATE pair, reducing DB round-trips
            // from 2*N to N for a typical autosave payload.
            jlptAttemptAnswerRepository.upsertAnswer(attemptId, questionId, normalizedOptionKey);
            savedCount++;
        }

        return JlptSaveAnswersResponseDTO.builder()
                .attemptId(attemptId)
                .savedCount(savedCount)
                .build();
    }

    @Override
    @Transactional
    public JlptSubmitAttemptResponseDTO submitAttempt(Long attemptId, Long userId) {
        JlptAttempt attempt = getAttemptOrThrow(attemptId, userId);
        ensureAttemptInProgress(attempt);
        if (getRemainingSeconds(attempt) <= 0) {
            autoSubmitExpiredAttempt(attempt);
            return JlptSubmitAttemptResponseDTO.builder()
                    .attemptId(attempt.getId())
                    .status(attempt.getStatus())
                    .totalScaledScore(attempt.getTotalScaledScore())
                    .passed(attempt.getPassed())
                    .build();
        }

        ComputedResult computed = computeResult(attempt);

        attempt.setStatus(JlptAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setTotalScaledScore(computed.totalScaledScore());
        attempt.setPassed(computed.passed());
        jlptAttemptRepository.save(attempt);

        return JlptSubmitAttemptResponseDTO.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus())
                .totalScaledScore(attempt.getTotalScaledScore())
                .passed(attempt.getPassed())
                .build();
    }

    @Override
    @Transactional
    public JlptAttemptResultDTO getAttemptResult(Long attemptId, Long userId) {
        JlptAttempt attempt = getAttemptOrThrow(attemptId, userId);
        if (attempt.getStatus() == JlptAttemptStatus.IN_PROGRESS && getRemainingSeconds(attempt) <= 0) {
            autoSubmitExpiredAttempt(attempt);
            attempt = getAttemptOrThrow(attemptId, userId);
        }
        if (attempt.getStatus() != JlptAttemptStatus.SUBMITTED) {
            throw new ApiException(ErrorCode.JLPT_RESULT_NOT_AVAILABLE);
        }

        ComputedResult computed = computeResult(attempt);

        return JlptAttemptResultDTO.builder()
                .attemptId(attempt.getId())
                .examId(attempt.getExam().getId())
                .examCode(attempt.getExam().getCode())
                .examTitle(attempt.getExam().getTitle())
                .level(attempt.getExam().getLevel())
                .totalScaledScore(computed.totalScaledScore())
                .passed(computed.passed())
                .sections(computed.sections())
                .build();
    }

    @Override
    @Transactional
    public List<JlptAttemptSummaryDTO> getAttemptHistory(Long userId) {
        return jlptAttemptRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .peek(attempt -> {
                    if (attempt.getStatus() == JlptAttemptStatus.IN_PROGRESS && getRemainingSeconds(attempt) <= 0) {
                        autoSubmitExpiredAttempt(attempt);
                    }
                })
                .map(attempt -> JlptAttemptSummaryDTO.builder()
                        .attemptId(attempt.getId())
                        .examId(attempt.getExam().getId())
                        .examCode(attempt.getExam().getCode())
                        .examTitle(attempt.getExam().getTitle())
                        .level(attempt.getExam().getLevel())
                        .status(attempt.getStatus())
                        .totalScaledScore(attempt.getTotalScaledScore())
                        .passed(attempt.getPassed())
                        .startedAt(attempt.getStartedAt())
                        .submittedAt(attempt.getSubmittedAt())
                        .build())
                .toList();
    }

    private JlptExam getExamOrThrow(Long examId) {
        JlptExam exam = jlptExamRepository.findById(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.JLPT_EXAM_NOT_FOUND));

        if (!exam.isPublished() || exam.getContentStatus() != JlptContentStatus.COMPLETE) {
            throw new ApiException(ErrorCode.JLPT_EXAM_NOT_PUBLISHED);
        }

        return exam;
    }

    private JlptAttempt getAttemptOrThrow(Long attemptId, Long userId) {
        return jlptAttemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.JLPT_ATTEMPT_NOT_FOUND));
    }

    private void ensureAttemptInProgress(JlptAttempt attempt) {
        if (attempt.getStatus() != JlptAttemptStatus.IN_PROGRESS) {
            throw new ApiException(ErrorCode.JLPT_ATTEMPT_ALREADY_SUBMITTED);
        }
    }

    private void ensureAttemptNotExpired(JlptAttempt attempt) {
        if (getRemainingSeconds(attempt) <= 0) {
            autoSubmitExpiredAttempt(attempt);
            throw new ApiException(ErrorCode.JLPT_EXPIRED);
        }
    }

    private int getRemainingSeconds(JlptAttempt attempt) {
        LocalDateTime now = LocalDateTime.now();
        if (attempt.getExpiresAt() != null) {
            long remaining = Duration.between(now, attempt.getExpiresAt()).getSeconds();
            return Math.max(0, (int) remaining);
        }
        long elapsedSeconds = Duration.between(attempt.getStartedAt(), now).getSeconds();
        // Give 5 minutes grace period for legacy attempts without expiresAt
        int totalSeconds = (attempt.getExam().getTotalDurationMinutes() + 5) * 60;
        return Math.max(0, (int) (totalSeconds - elapsedSeconds));
    }

    private void autoSubmitExpiredAttempt(JlptAttempt attempt) {
        if (attempt.getStatus() != JlptAttemptStatus.IN_PROGRESS) {
            return;
        }

        ComputedResult computed = computeResult(attempt);
        attempt.setStatus(JlptAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setTotalScaledScore(computed.totalScaledScore());
        attempt.setPassed(computed.passed());
        jlptAttemptRepository.save(attempt);
    }

    private String normalizeOptionKey(String optionKey) {
        if (optionKey == null) {
            return null;
        }
        return optionKey.trim().toUpperCase(Locale.ROOT);
    }

    private Set<Long> getQuestionIdsBySections(List<JlptSection> sections) {
        if (sections.isEmpty()) {
            return Set.of();
        }

        List<Long> sectionIds = sections.stream().map(JlptSection::getId).toList();
        List<JlptQuestion> questions = jlptQuestionRepository.findBySectionIdInOrderByQuestionNumberAsc(sectionIds);
        Set<Long> result = new HashSet<>();
        for (JlptQuestion question : questions) {
            result.add(question.getId());
        }
        return result;
    }

    private Map<Long, Set<String>> getValidOptionKeysByQuestionId(Set<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Set<String>> result = new HashMap<>();
        for (JlptQuestionOption option : jlptQuestionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(questionIds)) {
            Long questionId = option.getQuestion().getId();
            result.computeIfAbsent(questionId, ignored -> new HashSet<>())
                    .add(normalizeOptionKey(option.getOptionKey()));
        }
        return result;
    }

    private ComputedResult computeResult(JlptAttempt attempt) {
        Long examId = attempt.getExam().getId();
        List<JlptSection> sections = jlptSectionRepository.findByExamIdOrderBySectionOrderAsc(examId);
        if (sections.isEmpty()) {
            return new ComputedResult(List.of(), 0, false);
        }

        List<Long> sectionIds = sections.stream().map(JlptSection::getId).toList();
        List<JlptQuestion> questions = jlptQuestionRepository.findBySectionIdInOrderByQuestionNumberAsc(sectionIds);
        List<Long> questionIds = questions.stream().map(JlptQuestion::getId).toList();

        Map<Long, JlptAnswerKey> answerKeyByQuestionId = mapAnswerKeys(questionIds);
        Map<Long, JlptAttemptAnswer> attemptAnswerByQuestionId = mapAttemptAnswers(attempt.getId());
        Map<Long, List<JlptQuestionOption>> optionsByQuestionId = groupOptionsByQuestionId(questionIds);

        Map<Long, List<JlptQuestion>> questionsBySectionId = new LinkedHashMap<>();
        for (JlptQuestion question : questions) {
            Long sectionId = question.getSection().getId();
            questionsBySectionId.computeIfAbsent(sectionId, ignored -> new ArrayList<>()).add(question);
        }

        // Resolve pass thresholds for the exam's JLPT level.
        String level = attempt.getExam().getLevel();
        int[] thresholds = LEVEL_PASS_THRESHOLDS.getOrDefault(level, DEFAULT_PASS_THRESHOLDS);
        int passTotal = thresholds[0];
        int passMinSection = thresholds[1];

        List<JlptAttemptSectionResultDTO> sectionResults = new ArrayList<>();
        int totalScaled = 0;
        boolean meetsSectionRule = true;

        for (JlptSection section : sections) {
            List<JlptQuestion> sectionQuestions = questionsBySectionId.getOrDefault(section.getId(), List.of());
            SectionScoring sectionScoring = scoreSection(section, sectionQuestions, answerKeyByQuestionId, attemptAnswerByQuestionId, optionsByQuestionId);

            totalScaled += sectionScoring.scaledScore();
            if (sectionScoring.scaledScore() < passMinSection) {
                meetsSectionRule = false;
            }

            sectionResults.add(sectionScoring.sectionResult());
        }

        boolean passed = totalScaled >= passTotal && meetsSectionRule;
        return new ComputedResult(sectionResults, totalScaled, passed);
    }

    private SectionScoring scoreSection(
            JlptSection section,
            List<JlptQuestion> questions,
            Map<Long, JlptAnswerKey> answerKeyByQuestionId,
            Map<Long, JlptAttemptAnswer> attemptAnswerByQuestionId,
            Map<Long, List<JlptQuestionOption>> optionsByQuestionId
    ) {
        int rawScore = 0;
        int rawMaxScore = 0;
        List<JlptAttemptResultQuestionDTO> questionResults = new ArrayList<>();

        for (JlptQuestion question : questions) {
            JlptAnswerKey answerKey = answerKeyByQuestionId.get(question.getId());
            if (answerKey == null) {
                continue;
            }

            int weight = answerKey.getScoreWeight() == null ? 1 : answerKey.getScoreWeight();
            rawMaxScore += weight;

            JlptAttemptAnswer attemptAnswer = attemptAnswerByQuestionId.get(question.getId());
            String selectedOption = attemptAnswer == null ? null : normalizeOptionKey(attemptAnswer.getSelectedOptionKey());
            String correctOption = normalizeOptionKey(answerKey.getCorrectOptionKey());
            boolean correct = selectedOption != null && selectedOption.equals(correctOption);

            if (correct) {
                rawScore += weight;
            }

            List<JlptQuestionOptionDTO> optionDTOs = optionsByQuestionId.getOrDefault(question.getId(), List.of()).stream()
                    .map(option -> JlptQuestionOptionDTO.builder()
                            .key(option.getOptionKey())
                            .text(option.getOptionText())
                            .build())
                    .toList();

            questionResults.add(JlptAttemptResultQuestionDTO.builder()
                    .questionId(question.getId())
                    .questionNumber(question.getQuestionNumber())
                    .prompt(question.getPrompt())
                    .passageText(question.getPassageText())
                    .audioUrl(question.getAudioUrl())
                    .explanation(question.getExplanation())
                    .selectedOptionKey(selectedOption)
                    .correctOptionKey(correctOption)
                    .correct(correct)
                    .options(optionDTOs)
                    .build());
        }

        int scaled = rawMaxScore == 0 ? 0 : (int) Math.round((rawScore * 1.0 / rawMaxScore) * MAX_SECTION_SCALE);

        JlptAttemptSectionResultDTO sectionResult = JlptAttemptSectionResultDTO.builder()
                .sectionId(section.getId())
                .sectionType(section.getSectionType())
                .title(section.getTitle())
                .rawScore(rawScore)
                .rawMaxScore(rawMaxScore)
                .scaledScore(scaled)
                .scaledMaxScore(MAX_SECTION_SCALE)
                .questions(questionResults)
                .build();

        return new SectionScoring(sectionResult, scaled);
    }

    private Map<Long, List<JlptQuestionOption>> groupOptionsByQuestionId(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        List<JlptQuestionOption> options = jlptQuestionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(questionIds);
        Map<Long, List<JlptQuestionOption>> result = new LinkedHashMap<>();
        for (JlptQuestionOption option : options) {
            Long questionId = option.getQuestion().getId();
            result.computeIfAbsent(questionId, ignored -> new ArrayList<>()).add(option);
        }
        return result;
    }

    private Map<Long, JlptAnswerKey> mapAnswerKeys(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, JlptAnswerKey> result = new HashMap<>();
        for (JlptAnswerKey answerKey : jlptAnswerKeyRepository.findByQuestionIdIn(questionIds)) {
            result.put(answerKey.getQuestion().getId(), answerKey);
        }
        return result;
    }

    private Map<Long, JlptAttemptAnswer> mapAttemptAnswers(Long attemptId) {
        Map<Long, JlptAttemptAnswer> result = new HashMap<>();
        for (JlptAttemptAnswer answer : jlptAttemptAnswerRepository.findByAttemptId(attemptId)) {
            result.put(answer.getQuestion().getId(), answer);
        }
        return result;
    }

    private List<JlptSectionDTO> toSectionDTOs(List<JlptSection> sections) {
        if (sections.isEmpty()) {
            return List.of();
        }

        List<Long> sectionIds = sections.stream().map(JlptSection::getId).toList();
        List<JlptQuestion> questions = jlptQuestionRepository.findBySectionIdInOrderByQuestionNumberAsc(sectionIds);
        List<Long> questionIds = questions.stream().map(JlptQuestion::getId).toList();
        Map<Long, List<JlptQuestionOption>> optionsByQuestion = groupOptionsByQuestionId(questionIds);

        Map<Long, List<JlptQuestion>> questionBySection = new LinkedHashMap<>();
        for (JlptQuestion question : questions) {
            Long sectionId = question.getSection().getId();
            questionBySection.computeIfAbsent(sectionId, ignored -> new ArrayList<>()).add(question);
        }

        List<JlptSectionDTO> result = new ArrayList<>();
        for (JlptSection section : sections) {
            List<JlptQuestionDTO> questionDTOs = questionBySection.getOrDefault(section.getId(), List.of()).stream()
                    .map(question -> JlptQuestionDTO.builder()
                            .id(question.getId())
                            .partNumber(question.getPartNumber())
                            .questionNumber(question.getQuestionNumber())
                            .prompt(question.getPrompt())
                            .passageText(question.getPassageText())
                            .audioUrl(question.getAudioUrl())
                            .explanation(question.getExplanation())
                            .options(optionsByQuestion.getOrDefault(question.getId(), List.of()).stream()
                                    .map(option -> JlptQuestionOptionDTO.builder()
                                            .key(option.getOptionKey())
                                            .text(option.getOptionText())
                                            .build())
                                    .toList())
                            .build())
                    .toList();

            result.add(JlptSectionDTO.builder()
                    .id(section.getId())
                    .sectionType(section.getSectionType())
                    .title(section.getTitle())
                    .sectionOrder(section.getSectionOrder())
                    .durationMinutes(section.getDurationMinutes())
                    .questions(questionDTOs)
                    .build());
        }

        return result;
    }

    private record ComputedResult(List<JlptAttemptSectionResultDTO> sections, int totalScaledScore, boolean passed) {
    }

    private record SectionScoring(JlptAttemptSectionResultDTO sectionResult, int scaledScore) {
    }

    @Override
    @Transactional(readOnly = true)
    public List<JlptQuestionDTO> getPracticeQuestions(String level, String sectionType, int limit) {
        JlptSectionType typeEnum;
        try {
            typeEnum = JlptSectionType.valueOf(sectionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR);
        }

        List<JlptQuestion> questions = jlptQuestionRepository.findBySection_Exam_LevelAndSection_SectionType(level.toUpperCase(), typeEnum);
        
        // Shuffle to get random questions for practice
        List<JlptQuestion> shuffled = new ArrayList<>(questions);
        Collections.shuffle(shuffled);
        if (shuffled.size() > limit) {
            shuffled = shuffled.subList(0, limit);
        }

        List<Long> questionIds = shuffled.stream().map(JlptQuestion::getId).toList();
        Map<Long, List<JlptQuestionOption>> optionsByQuestion = groupOptionsByQuestionId(questionIds);

        Map<Long, JlptAnswerKey> answerKeys = mapAnswerKeys(questionIds);

        return shuffled.stream().map(question -> {
            JlptAnswerKey key = answerKeys.get(question.getId());
            return JlptQuestionDTO.builder()
                .id(question.getId())
                .partNumber(question.getPartNumber())
                .questionNumber(question.getQuestionNumber())
                .prompt(question.getPrompt())
                .passageText(question.getPassageText())
                .audioUrl(question.getAudioUrl())
                .explanation(question.getExplanation())
                .correctOptionKey(key != null ? key.getCorrectOptionKey() : null)
                .options(optionsByQuestion.getOrDefault(question.getId(), List.of()).stream()
                        .map(option -> JlptQuestionOptionDTO.builder()
                                .key(option.getOptionKey())
                                .text(option.getOptionText())
                                .build())
                        .toList())
                .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String evaluatePlacementTest(JlptSaveAnswersRequest request) {
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return "N5";
        }
        
        int correctCount = 0;
        int totalQuestions = request.getAnswers().size();
        
        List<Long> questionIds = request.getAnswers().stream().map(JlptSaveAnswersRequest.AnswerItem::getQuestionId).toList();
        Map<Long, JlptAnswerKey> answerKeys = mapAnswerKeys(questionIds);

        for (JlptSaveAnswersRequest.AnswerItem item : request.getAnswers()) {
            JlptAnswerKey key = answerKeys.get(item.getQuestionId());
            if (key != null && key.getCorrectOptionKey().equalsIgnoreCase(item.getSelectedOptionKey())) {
                correctCount++;
            }
        }
        
        double accuracy = (double) correctCount / totalQuestions;
        if (accuracy > 0.8) return "N2";
        if (accuracy > 0.6) return "N3";
        if (accuracy > 0.4) return "N4";
        return "N5";
    }
}
