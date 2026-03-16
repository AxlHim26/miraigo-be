package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JlptQuestionOptionDTO {
    private String key;
    private String text;
}
