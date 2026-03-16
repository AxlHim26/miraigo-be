package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JlptExamListItemDTO {
    private Long id;
    private String code;
    private String title;
    private String level;
    private Integer examYear;
    private Integer examMonth;
    private Integer totalDurationMinutes;
}
