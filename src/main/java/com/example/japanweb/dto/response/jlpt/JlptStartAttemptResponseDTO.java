package com.example.japanweb.dto.response.jlpt;

import com.example.japanweb.entity.JlptAttemptStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JlptStartAttemptResponseDTO {
    private Long attemptId;
    private Long examId;
    private JlptAttemptStatus status;
    private LocalDateTime startedAt;
    private Integer totalDurationMinutes;
    private Integer remainingSeconds;
    private List<JlptAttemptAnswerDTO> answers;
}
