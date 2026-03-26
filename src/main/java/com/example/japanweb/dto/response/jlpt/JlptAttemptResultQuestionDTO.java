package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JlptAttemptResultQuestionDTO {
    private Long questionId;
    private Integer questionNumber;
    private String prompt;
    private String explanation;
    private String selectedOptionKey;
    private String correctOptionKey;
    private boolean correct;
    private List<JlptQuestionOptionDTO> options;
}
