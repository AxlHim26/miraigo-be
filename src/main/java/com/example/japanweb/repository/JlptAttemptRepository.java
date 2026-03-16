package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptAttempt;
import com.example.japanweb.entity.JlptAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JlptAttemptRepository extends JpaRepository<JlptAttempt, Long> {

    List<JlptAttempt> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<JlptAttempt> findByIdAndUserId(Long id, Long userId);

    Optional<JlptAttempt> findTopByExamIdAndUserIdAndStatusOrderByCreatedAtDesc(
            Long examId,
            Long userId,
            JlptAttemptStatus status
    );
}
