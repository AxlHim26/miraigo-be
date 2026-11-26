package com.example.japanweb;

import com.example.japanweb.service.JlptService;
import com.example.japanweb.dto.request.jlpt.JlptSaveAnswersRequest;
import com.example.japanweb.entity.User;
import com.example.japanweb.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs against a database created by Hibernate alone, like a fresh installation. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JlptSaveAnswersTest {

    @Autowired
    private JlptService jlptService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savingAnAnswerTwiceKeepsOnlyTheLatestChoice() {
        Long userId = userRepository.save(User.builder()
                .username("jlpt-save-test")
                .email("jlpt-save-test@example.com")
                .passwordHash("unused")
                .build()).getId();
        Long examId = jlptService.getPublishedExams().get(0).getId();
        var question = jlptService.getExamDetail(examId).getSections().get(0).getQuestions().get(0);
        Long attemptId = jlptService.startAttempt(examId, userId).getAttemptId();

        save(attemptId, userId, question.getId(), question.getOptions().get(0).getKey());
        save(attemptId, userId, question.getId(), question.getOptions().get(1).getKey());

        assertThat(jlptService.getAttemptSession(attemptId, userId).getAnswers())
                .singleElement()
                .satisfies(answer -> assertThat(answer.getSelectedOptionKey())
                        .isEqualTo(question.getOptions().get(1).getKey()));
    }

    private void save(Long attemptId, Long userId, Long questionId, String optionKey) {
        jlptService.saveAnswers(attemptId, userId, JlptSaveAnswersRequest.builder()
                .answers(List.of(JlptSaveAnswersRequest.AnswerItem.builder()
                        .questionId(questionId)
                        .selectedOptionKey(optionKey)
                        .build()))
                .build());
    }
}
