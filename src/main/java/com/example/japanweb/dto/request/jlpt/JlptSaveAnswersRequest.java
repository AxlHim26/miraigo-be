package com.example.japanweb.dto.request.jlpt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JlptSaveAnswersRequest {

    @NotEmpty(message = "answers is required")
    @Valid
    private List<AnswerItem> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerItem {
        @NotNull(message = "questionId is required")
        private Long questionId;

        @NotNull(message = "selectedOptionKey is required")
        private String selectedOptionKey;
    }
}
