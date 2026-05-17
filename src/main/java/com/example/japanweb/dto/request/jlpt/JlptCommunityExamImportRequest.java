package com.example.japanweb.dto.request.jlpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JlptCommunityExamImportRequest {

    @NotBlank(message = "Level is required")
    private String level;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Sections are required")
    private List<JlptParsedExamImportRequest.ParsedSection> sections;
}
