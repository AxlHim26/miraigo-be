package com.example.japanweb.dto.request.jlpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Request body for POST /api/v1/admin/jlpt/import-parsed-exam.
 * Accepts parsed exam data (as produced by parse-jlpt-questions.mjs) and
 * populates the database with questions, options, and answer keys.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JlptParsedExamImportRequest {

    /**
     * The exam code to import into (e.g. "N4-2023-07").
     * The exam record must already exist in jlpt_exams.
     */
    private String examCode;

    /**
     * Whether to replace existing question data for this exam (default: true).
     * If false and questions already exist, the import will fail.
     */
    private boolean replaceExisting = true;

    /**
     * The parsed sections from the JSON output of parse-jlpt-questions.mjs.
     */
    private List<ParsedSection> sections;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParsedSection {
        /** VOCABULARY, GRAMMAR_READING, or LISTENING */
        private String type;
        private List<ParsedProblem> problems;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParsedProblem {
        private String problemNumber;
        private List<ParsedQuestion> questions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParsedQuestion {
        private String number;
        private String prompt;
        private String explanation;
        private List<ParsedOption> options;
        private String correctAnswer;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParsedOption {
        private String key;
        private String text;
    }
}
