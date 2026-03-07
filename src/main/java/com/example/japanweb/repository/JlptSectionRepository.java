package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JlptSectionRepository extends JpaRepository<JlptSection, Long> {

    List<JlptSection> findByExamIdOrderBySectionOrderAsc(Long examId);
}
