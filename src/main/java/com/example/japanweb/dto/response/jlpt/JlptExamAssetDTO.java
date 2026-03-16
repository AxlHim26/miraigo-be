package com.example.japanweb.dto.response.jlpt;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JlptExamAssetDTO {
    private Long id;
    private String assetType;
    private String sourcePath;
    private String extractedTextPath;
    private String quality;
}
