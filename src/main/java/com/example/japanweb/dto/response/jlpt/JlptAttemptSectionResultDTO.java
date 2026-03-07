package com.example.japanweb.dto.response.jlpt;

import com.example.japanweb.entity.JlptSectionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JlptAttemptSectionResultDTO {
    private Long sectionId;
    private JlptSectionType sectionType;
    private String title;
    private Integer rawScore;
    private Integer rawMaxScore;
    private Integer scaledScore;
    private List<JlptAttemptResultQuestionDTO> questions;
}
