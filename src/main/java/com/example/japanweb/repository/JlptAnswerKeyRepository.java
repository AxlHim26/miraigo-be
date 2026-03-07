package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptAnswerKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JlptAnswerKeyRepository extends JpaRepository<JlptAnswerKey, Long> {

    List<JlptAnswerKey> findByQuestionIdIn(Collection<Long> questionIds);
}
