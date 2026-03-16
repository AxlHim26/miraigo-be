package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptContentStatus;
import com.example.japanweb.entity.JlptExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JlptExamRepository extends JpaRepository<JlptExam, Long> {

    List<JlptExam> findByPublishedTrueOrderByExamYearDescExamMonthDescLevelAsc();

    List<JlptExam> findByPublishedTrueAndContentStatusOrderByExamYearDescExamMonthDescLevelAsc(JlptContentStatus contentStatus);

    Optional<JlptExam> findByCode(String code);
}
