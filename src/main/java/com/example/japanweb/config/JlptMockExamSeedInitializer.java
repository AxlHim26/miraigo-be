package com.example.japanweb.config;

import com.example.japanweb.service.JlptMockExamSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Loads bundled JLPT mock exams (mock_tests/*.json) after the application context is ready.
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class JlptMockExamSeedInitializer implements ApplicationRunner {

    private final JlptMockExamSeedService jlptMockExamSeedService;

    @Override
    public void run(ApplicationArguments args) {
        jlptMockExamSeedService.seedFromClasspathIfNeeded();
    }
}
