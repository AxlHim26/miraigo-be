package com.example.japanweb.dto.response.jlpt;

import com.example.japanweb.entity.JlptAttemptStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JlptSubmitAttemptResponseDTO {
    private Long attemptId;
    private JlptAttemptStatus status;
    private Integer totalScaledScore;
    private Boolean passed;
}
