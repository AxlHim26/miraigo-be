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
    /** Maximum possible scaled score for this section (always {@code MAX_SECTION_SCALE}). */
    private Integer scaledMaxScore;
    private List<JlptAttemptResultQuestionDTO> questions;
}
