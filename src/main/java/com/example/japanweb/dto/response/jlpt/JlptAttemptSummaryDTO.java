package com.example.japanweb.dto.response.jlpt;

import com.example.japanweb.entity.JlptAttemptStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JlptAttemptSummaryDTO {
    private Long attemptId;
    private Long examId;
    private String examCode;
    private String examTitle;
    private String level;
    private JlptAttemptStatus status;
    private Integer totalScaledScore;
    private Boolean passed;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
}
