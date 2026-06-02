package com.example.japanweb.service.impl;

import com.example.japanweb.config.properties.JlptMockSeedProperties;
import com.example.japanweb.dto.request.jlpt.JlptParsedExamImportRequest;
import com.example.japanweb.dto.response.jlpt.JlptParsedExamImportResultDTO;
import com.example.japanweb.entity.JlptContentStatus;
import com.example.japanweb.entity.JlptExam;
import com.example.japanweb.repository.JlptExamRepository;
import com.example.japanweb.repository.JlptSectionRepository;
import com.example.japanweb.service.JlptImportService;
import com.example.japanweb.service.JlptMockExamSeedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JlptMockExamSeedServiceImpl implements JlptMockExamSeedService {

    private record MockExamDefinition(
            String code,
            String title,
            String level,
            int examYear,
            int examMonth,
            int totalDurationMinutes
    ) {
    }

    private static final Map<String, MockExamDefinition> KNOWN_MOCK_EXAMS = Map.of(
            "N5-MOCK-1", new MockExamDefinition("N5-MOCK-1", "JLPT N5 Mock Test 1", "N5", 2026, 1, 90),
            "N4-MOCK-1", new MockExamDefinition("N4-MOCK-1", "JLPT N4 Mock Test 1", "N4", 2026, 1, 125),
            "N3-MOCK-1", new MockExamDefinition("N3-MOCK-1", "JLPT N3 Mock Test 1", "N3", 2026, 1, 140),
            "N2-MOCK-1", new MockExamDefinition("N2-MOCK-1", "JLPT N2 Mock Test 1", "N2", 2026, 1, 155),
            "N1-MOCK-1", new MockExamDefinition("N1-MOCK-1", "JLPT N1 Mock Test 1", "N1", 2026, 1, 170)
    );

    private final JlptMockSeedProperties properties;
    private final JlptImportService jlptImportService;
    private final JlptExamRepository jlptExamRepository;
    private final JlptSectionRepository jlptSectionRepository;
    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourcePatternResolver;

    @Override
    @Transactional
    public void seedFromClasspathIfNeeded() {
        if (!properties.isEnabled()) {
            log.debug("JLPT mock exam seeding is disabled.");
            return;
        }

        String pattern = normalizeLocationPattern(properties.getLocation());
        Resource[] resources;
        try {
            resources = resourcePatternResolver.getResources(pattern);
        } catch (IOException exception) {
            log.warn("Could not read JLPT mock exam resources from '{}': {}", pattern, exception.getMessage());
            return;
        }

        if (resources.length == 0) {
            log.warn("No JLPT mock exam JSON files found at '{}'.", pattern);
            return;
        }

        Arrays.sort(resources, Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)));

        int imported = 0;
        int skipped = 0;

        for (Resource resource : resources) {
            if (!resource.exists() || !resource.isReadable()) {
                continue;
            }

            try {
                if (seedResource(resource)) {
                    imported++;
                } else {
                    skipped++;
                }
            } catch (IOException exception) {
                log.error("Failed to seed JLPT mock exam from {}: {}", resource.getFilename(), exception.getMessage());
            }
        }

        log.info("JLPT mock exam seed finished: {} imported, {} skipped.", imported, skipped);
    }

    private boolean seedResource(Resource resource) throws IOException {
        JlptParsedExamImportRequest request;
        try (InputStream inputStream = resource.getInputStream()) {
            request = objectMapper.readValue(inputStream, JlptParsedExamImportRequest.class);
        }

        if (request.getExamCode() == null || request.getExamCode().isBlank()) {
            log.warn("Skipping {} because examCode is missing.", resource.getFilename());
            return false;
        }

        if (request.getSections() == null || request.getSections().isEmpty()) {
            log.warn("Skipping {} because sections are empty.", resource.getFilename());
            return false;
        }

        MockExamDefinition definition = resolveDefinition(request.getExamCode());
        JlptExam exam = ensureExam(definition);

        if (!needsImport(exam)) {
            log.debug("JLPT mock exam '{}' is already complete; skipping {}.", exam.getCode(), resource.getFilename());
            return false;
        }

        request.setReplaceExisting(true);
        JlptParsedExamImportResultDTO result = jlptImportService.importParsedExam(request);

        log.info(
                "Seeded JLPT mock exam '{}' from {} ({} sections, {} questions, {} skipped).",
                exam.getCode(),
                resource.getFilename(),
                result.getImportedSections(),
                result.getImportedQuestions(),
                result.getSkippedQuestions()
        );

        if (!result.getWarnings().isEmpty()) {
            log.debug("JLPT mock seed warnings for '{}': {}", exam.getCode(), result.getWarnings());
        }

        return true;
    }

    private boolean needsImport(JlptExam exam) {
        if (exam.getContentStatus() != JlptContentStatus.COMPLETE) {
            return true;
        }
        return jlptSectionRepository.findByExamIdOrderBySectionOrderAsc(exam.getId()).isEmpty();
    }

    private JlptExam ensureExam(MockExamDefinition definition) {
        return jlptExamRepository.findByCode(definition.code())
                .map(existing -> updateExamMetadata(existing, definition))
                .orElseGet(() -> jlptExamRepository.save(
                        JlptExam.builder()
                                .code(definition.code())
                                .title(definition.title())
                                .level(definition.level())
                                .examYear(definition.examYear())
                                .examMonth(definition.examMonth())
                                .totalDurationMinutes(definition.totalDurationMinutes())
                                .published(true)
                                .contentStatus(JlptContentStatus.DRAFT)
                                .build()
                ));
    }

    private JlptExam updateExamMetadata(JlptExam exam, MockExamDefinition definition) {
        boolean changed = false;

        if (!definition.title().equals(exam.getTitle())) {
            exam.setTitle(definition.title());
            changed = true;
        }
        if (!definition.level().equals(exam.getLevel())) {
            exam.setLevel(definition.level());
            changed = true;
        }
        if (!Integer.valueOf(definition.examYear()).equals(exam.getExamYear())) {
            exam.setExamYear(definition.examYear());
            changed = true;
        }
        if (!Integer.valueOf(definition.examMonth()).equals(exam.getExamMonth())) {
            exam.setExamMonth(definition.examMonth());
            changed = true;
        }
        if (exam.getTotalDurationMinutes() != definition.totalDurationMinutes()) {
            exam.setTotalDurationMinutes(definition.totalDurationMinutes());
            changed = true;
        }
        if (!exam.isPublished()) {
            exam.setPublished(true);
            changed = true;
        }

        return changed ? jlptExamRepository.save(exam) : exam;
    }

    private MockExamDefinition resolveDefinition(String examCode) {
        MockExamDefinition known = KNOWN_MOCK_EXAMS.get(examCode);
        if (known != null) {
            return known;
        }

        String level = inferLevel(examCode);
        return new MockExamDefinition(
                examCode,
                "JLPT " + level + " Mock Test",
                level,
                2026,
                1,
                defaultDurationByLevel(level)
        );
    }

    private String inferLevel(String examCode) {
        if (examCode.length() >= 2 && examCode.startsWith("N")) {
            String candidate = examCode.substring(0, 2);
            if (List.of("N1", "N2", "N3", "N4", "N5").contains(candidate)) {
                return candidate;
            }
        }
        return "N5";
    }

    private int defaultDurationByLevel(String level) {
        return switch (level) {
            case "N1" -> 170;
            case "N2" -> 155;
            case "N3" -> 140;
            case "N4" -> 125;
            default -> 90;
        };
    }

    private String normalizeLocationPattern(String location) {
        String base = StringUtils.hasText(location) ? location.trim() : "classpath:mock_tests/";
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        if (base.startsWith("classpath:") && !base.contains("*")) {
            return base + "*.json";
        }
        if (!base.contains("*")) {
            return base + "*.json";
        }
        return base;
    }
}
