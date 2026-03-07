package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JlptAttemptResultDTO {
    private Long attemptId;
    private Long examId;
    private String examCode;
    private String examTitle;
    private String level;
    private Integer totalScaledScore;
    private Boolean passed;
    private List<JlptAttemptSectionResultDTO> sections;
}
