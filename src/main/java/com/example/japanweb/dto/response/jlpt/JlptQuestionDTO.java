package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JlptQuestionDTO {
    private Long id;
    private Integer partNumber;
    private Integer questionNumber;
    private String prompt;
    private String passageText;
    private String audioUrl;
    private String explanation;
    private String correctOptionKey;
    private List<JlptQuestionOptionDTO> options;
}
