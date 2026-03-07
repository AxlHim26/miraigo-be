package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JlptQuestionOptionRepository extends JpaRepository<JlptQuestionOption, Long> {

    List<JlptQuestionOption> findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(Collection<Long> questionIds);
}
