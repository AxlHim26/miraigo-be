package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JlptAttemptAnswerRepository extends JpaRepository<JlptAttemptAnswer, Long> {

    List<JlptAttemptAnswer> findByAttemptId(Long attemptId);

    List<JlptAttemptAnswer> findByAttemptIdIn(Collection<Long> attemptIds);

    Optional<JlptAttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
