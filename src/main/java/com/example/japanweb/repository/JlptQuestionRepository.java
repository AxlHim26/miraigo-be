package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JlptQuestionRepository extends JpaRepository<JlptQuestion, Long> {

    List<JlptQuestion> findBySectionIdInOrderByQuestionNumberAsc(List<Long> sectionIds);

    List<JlptQuestion> findBySection_Exam_LevelAndSection_SectionType(String level, com.example.japanweb.entity.JlptSectionType sectionType);

    List<JlptQuestion> findBySection_Exam_Level(String level);
}
