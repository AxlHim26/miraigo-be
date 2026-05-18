package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptSectionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JlptSectionAttemptRepository extends JpaRepository<JlptSectionAttempt, Long> {
    Optional<JlptSectionAttempt> findByAttemptIdAndSectionId(Long attemptId, Long sectionId);
    List<JlptSectionAttempt> findByAttemptId(Long attemptId);
}
