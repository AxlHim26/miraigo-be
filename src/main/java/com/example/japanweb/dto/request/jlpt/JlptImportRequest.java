package com.example.japanweb.dto.request.jlpt;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JlptImportRequest {

    @NotBlank(message = "manifestPath is required")
    private String manifestPath;

    @Builder.Default
    private boolean overwriteExisting = true;
}
