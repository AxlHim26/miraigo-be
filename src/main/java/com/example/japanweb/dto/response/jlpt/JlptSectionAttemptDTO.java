package com.example.japanweb.dto.response.jlpt;

import com.example.japanweb.entity.JlptAttemptStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JlptSectionAttemptDTO {
    private Long sectionId;
    private String sectionStatus; // e.g. "NOT_STARTED", "IN_PROGRESS", "SUBMITTED"
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime expiresAt;
    private Integer remainingSeconds;
}
