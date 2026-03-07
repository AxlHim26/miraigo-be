package com.example.japanweb.controller;

import com.example.japanweb.dto.common.ApiResponse;
import com.example.japanweb.dto.request.jlpt.JlptSaveAnswersRequest;
import com.example.japanweb.dto.response.jlpt.*;
import com.example.japanweb.entity.User;
import com.example.japanweb.service.JlptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jlpt")
@RequiredArgsConstructor
public class JlptController {

    private final JlptService jlptService;

    @GetMapping("/exams")
    public ApiResponse<List<JlptExamListItemDTO>> getExams() {
        return ApiResponse.success(jlptService.getPublishedExams());
    }

    @GetMapping("/exams/{examId}")
    public ApiResponse<JlptExamDetailDTO> getExamDetail(@PathVariable Long examId) {
        return ApiResponse.success(jlptService.getExamDetail(examId));
    }

    @PostMapping("/exams/{examId}/attempts")
    public ApiResponse<JlptStartAttemptResponseDTO> startAttempt(
            @PathVariable Long examId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(jlptService.startAttempt(examId, user.getId()));
    }

    @PatchMapping("/attempts/{attemptId}/answers")
    public ApiResponse<JlptSaveAnswersResponseDTO> saveAnswers(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody JlptSaveAnswersRequest request
    ) {
        return ApiResponse.success(jlptService.saveAnswers(attemptId, user.getId(), request));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ApiResponse<JlptSubmitAttemptResponseDTO> submitAttempt(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(jlptService.submitAttempt(attemptId, user.getId()));
    }

    @GetMapping("/attempts/{attemptId}/result")
    public ApiResponse<JlptAttemptResultDTO> getResult(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(jlptService.getAttemptResult(attemptId, user.getId()));
    }

    @GetMapping("/attempts/history")
    public ApiResponse<List<JlptAttemptSummaryDTO>> getAttemptHistory(@AuthenticationPrincipal User user) {
        return ApiResponse.success(jlptService.getAttemptHistory(user.getId()));
    }
}
