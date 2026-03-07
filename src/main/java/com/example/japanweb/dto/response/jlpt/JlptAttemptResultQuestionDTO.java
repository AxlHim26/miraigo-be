package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JlptAttemptResultQuestionDTO {
    private Long questionId;
    private Integer questionNumber;
    private String prompt;
    private String selectedOptionKey;
    private String correctOptionKey;
    private boolean correct;
}
