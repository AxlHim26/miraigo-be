package com.example.japanweb.controller;

import com.example.japanweb.dto.common.ApiResponse;
import com.example.japanweb.dto.request.vocab.ReviewRequest;
import com.example.japanweb.dto.response.vocab.ReviewResponse;
import com.example.japanweb.dto.response.vocab.ReviewStatsDTO;
import com.example.japanweb.dto.response.vocab.VocabCourseDTO;
import com.example.japanweb.dto.response.vocab.VocabCourseDetailDTO;
import com.example.japanweb.dto.response.vocab.VocabEntryDTO;
import com.example.japanweb.dto.response.vocab.VocabProgressDTO;
import com.example.japanweb.entity.User;
import com.example.japanweb.entity.UserLearningProgress;
import com.example.japanweb.repository.UserLearningProgressRepository;
import com.example.japanweb.service.SrsService;
import com.example.japanweb.service.VocabService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for Vocabulary operations.
 */
@RestController
@RequestMapping("/api/v1/vocab")
@RequiredArgsConstructor
@Validated
public class VocabController {

    private final VocabService vocabService;
    private final SrsService srsService;
    private final UserLearningProgressRepository progressRepository;

    /**
     * Get all vocabulary courses with pagination.
     */
    @GetMapping("/courses")
    public ApiResponse<Page<VocabCourseDTO>> getCourses(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Page<VocabCourseDTO> courses = vocabService.getAllCourses(PageRequest.of(page, size));
        return ApiResponse.success(courses);
    }

    /**
     * Get vocabulary course by ID with all entries.
     */
    @GetMapping("/courses/{id}")
    public ApiResponse<VocabCourseDetailDTO> getCourseById(@PathVariable Long id) {
        VocabCourseDetailDTO course = vocabService.getCourseWithEntries(id);
        return ApiResponse.success(course);
    }

    /**
     * Get vocabulary entries due for review (SRS).
     */
    @GetMapping("/review")
    public ApiResponse<List<VocabEntryDTO>> getDueForReview(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

        List<UserLearningProgress> dueProgress = progressRepository
                .findDueForReviewWithLimit(user.getId(), LocalDateTime.now(), limit);

        List<VocabEntryDTO> dueEntries = dueProgress.stream()
                .map(p -> VocabEntryDTO.builder()
                        .id(p.getVocab().getId())
                        .term(p.getVocab().getTerm())
                        .reading(p.getVocab().getReading())
                        .meaning(p.getVocab().getMeaning())
                        .example(p.getVocab().getExample())
                        .level(p.getVocab().getLevel())
                        .build())
                .toList();

        return ApiResponse.success(dueEntries);
    }

    /**
     * Submit a review rating for vocabulary.
     */
    @PostMapping("/review")
    public ApiResponse<ReviewResponse> submitReview(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReviewRequest request) {

        // Get or create progress
        UserLearningProgress progress = srsService.getOrCreateProgress(
                user.getId(), request.getVocabId());

        // Process the review with SRS
        SrsService.Rating rating = SrsService.Rating.fromValue(request.getRating());
        progress = srsService.processReview(progress, rating);

        ReviewResponse response = ReviewResponse.builder()
                .vocabId(request.getVocabId())
                .nextReviewAt(progress.getNextReviewAt())
                .intervalDays(progress.getIntervalDays())
                .message(getReviewMessage(rating))
                .build();

        return ApiResponse.success(response);
    }

    /**
     * Get review statistics for the current user.
     */
    @GetMapping("/stats")
    public ApiResponse<ReviewStatsDTO> getReviewStats(@AuthenticationPrincipal User user) {
        long totalLearning = progressRepository.countByUserId(user.getId());
        long dueForReview = progressRepository.countDueForReview(user.getId(), LocalDateTime.now());

        ReviewStatsDTO stats = ReviewStatsDTO.builder()
                .totalLearning(totalLearning)
                .dueForReview(dueForReview)
                .build();

        return ApiResponse.success(stats);
    }

    @GetMapping("/progress")
    public ApiResponse<List<VocabProgressDTO>> getProgress(@AuthenticationPrincipal User user) {
        return ApiResponse.success(progressRepository.findProgressByUserId(user.getId()));
    }

    private String getReviewMessage(SrsService.Rating rating) {
        return switch (rating) {
            case AGAIN -> "Card will be shown again soon. Keep practicing!";
            case HARD -> "Marked as difficult. You'll see this again sooner.";
            case GOOD -> "Good job! Card scheduled for later.";
            case EASY -> "Perfect! Card interval extended.";
        };
    }
}
