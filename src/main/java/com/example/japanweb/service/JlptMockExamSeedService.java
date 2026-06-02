package com.example.japanweb.service;

/**
 * Seeds published JLPT mock exams from bundled JSON files (mock_tests/*.json).
 */
public interface JlptMockExamSeedService {

    void seedFromClasspathIfNeeded();
}
