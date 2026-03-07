package com.example.japanweb.dto.response.jlpt;

import com.example.japanweb.entity.JlptSectionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JlptSectionDTO {
    private Long id;
    private JlptSectionType sectionType;
    private String title;
    private Integer sectionOrder;
    private Integer durationMinutes;
    private List<JlptQuestionDTO> questions;
}
