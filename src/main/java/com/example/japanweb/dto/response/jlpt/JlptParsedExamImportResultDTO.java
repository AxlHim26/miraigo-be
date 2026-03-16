package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JlptParsedExamImportResultDTO {
    private String examCode;
    private int importedSections;
    private int importedQuestions;
    private int skippedQuestions;
    private List<String> warnings;
}
