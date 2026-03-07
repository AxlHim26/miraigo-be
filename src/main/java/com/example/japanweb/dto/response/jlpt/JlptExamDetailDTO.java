package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JlptExamDetailDTO {
    private Long id;
    private String code;
    private String title;
    private String level;
    private Integer examYear;
    private Integer examMonth;
    private Integer totalDurationMinutes;
    private List<JlptExamAssetDTO> assets;
    private List<JlptSectionDTO> sections;
}
