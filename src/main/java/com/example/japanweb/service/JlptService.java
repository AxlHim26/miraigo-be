package com.example.japanweb.service;

import com.example.japanweb.dto.request.jlpt.JlptSaveAnswersRequest;
import com.example.japanweb.dto.response.jlpt.*;

import java.util.List;

public interface JlptService {

    List<JlptExamListItemDTO> getPublishedExams();

    JlptExamDetailDTO getExamDetail(Long examId);

    JlptStartAttemptResponseDTO startAttempt(Long examId, Long userId);

    JlptStartAttemptResponseDTO getAttemptSession(Long attemptId, Long userId);

    JlptSaveAnswersResponseDTO saveAnswers(Long attemptId, Long userId, JlptSaveAnswersRequest request);

    JlptSubmitAttemptResponseDTO submitAttempt(Long attemptId, Long userId);

    JlptAttemptResultDTO getAttemptResult(Long attemptId, Long userId);

    List<JlptAttemptSummaryDTO> getAttemptHistory(Long userId);

    List<JlptQuestionDTO> getPracticeQuestions(String level, String sectionType, int limit);

    String evaluatePlacementTest(JlptSaveAnswersRequest request);
}
