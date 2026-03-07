package com.example.japanweb.controller;

import com.example.japanweb.dto.common.ApiResponse;
import com.example.japanweb.dto.request.jlpt.JlptImportRequest;
import com.example.japanweb.dto.response.jlpt.JlptImportResultDTO;
import com.example.japanweb.dto.response.vocab.BulkImportResultDTO;
import com.example.japanweb.service.BulkImportService;
import com.example.japanweb.service.JlptImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin-only controller for bulk operations.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final BulkImportService bulkImportService;
    private final JlptImportService jlptImportService;

    /**
     * Import vocabulary entries from an Excel file.
     * 
     * Expected Excel format:
     * | Term | Reading | Meaning | Example | Level |
     * 
     * @param file     The Excel file (.xlsx)
     * @param courseId Target vocabulary course ID
     * @return Import result with success count and errors
     */
    @PostMapping("/vocab/import")
    public ApiResponse<BulkImportResultDTO> importVocabulary(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId) {

        BulkImportResultDTO result = bulkImportService.importVocabulary(file, courseId);

        String message = String.format("Import completed: %d/%d successful",
                result.getSuccessCount(), result.getTotalRows());

        return ApiResponse.success(result, message);
    }

    @PostMapping("/jlpt/import-manifest")
    public ApiResponse<JlptImportResultDTO> importJlptManifest(@Valid @RequestBody JlptImportRequest request) {
        JlptImportResultDTO result = jlptImportService.importFromManifest(request);
        String message = String.format("Imported %d exams and %d assets", result.getImportedExams(), result.getImportedAssets());
        return ApiResponse.success(result, message);
    }
}
