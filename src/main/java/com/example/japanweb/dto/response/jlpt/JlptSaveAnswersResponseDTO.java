package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JlptSaveAnswersResponseDTO {
    private Long attemptId;
    private Integer savedCount;
}
