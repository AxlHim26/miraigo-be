package com.example.japanweb.dto.response.vocab;

import java.time.LocalDateTime;

public record VocabProgressDTO(
        Long vocabId,
        Long courseId,
        LocalDateTime createdAt,
        LocalDateTime lastReviewedAt,
        LocalDateTime nextReviewAt,
        Integer repetitions) {
}
