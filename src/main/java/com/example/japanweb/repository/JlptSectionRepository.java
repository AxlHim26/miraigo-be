package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptSection;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JlptSectionRepository extends JpaRepository<JlptSection, Long> {

    List<JlptSection> findByExamIdOrderBySectionOrderAsc(Long examId);

    @Modifying
    @Transactional
    @Query("DELETE FROM JlptSection s WHERE s.exam.id = :examId")
    void deleteByExamId(@Param("examId") Long examId);
}
