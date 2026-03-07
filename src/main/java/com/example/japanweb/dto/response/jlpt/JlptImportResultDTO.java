package com.example.japanweb.dto.response.jlpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JlptImportResultDTO {
    private int totalFiles;
    private int importedExams;
    private int importedAssets;
    private List<String> warnings;
}
