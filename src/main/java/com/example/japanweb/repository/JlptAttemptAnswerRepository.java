package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JlptAttemptAnswerRepository extends JpaRepository<JlptAttemptAnswer, Long> {

    List<JlptAttemptAnswer> findByAttemptId(Long attemptId);

    List<JlptAttemptAnswer> findByAttemptIdIn(Collection<Long> attemptIds);

    Optional<JlptAttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    /**
     * Batch upsert a single answer row. Uses PostgreSQL's INSERT … ON CONFLICT DO UPDATE so
     * the entire autosave payload is handled in O(N) queries instead of 2*N round-trips.
     * The caller is responsible for normalising {@code selectedOptionKey} before calling this.
     */
    @Modifying
    @Query(value = """
            INSERT INTO jlpt_attempt_answers (attempt_id, question_id, selected_option_key, answered_at)
            VALUES (:attemptId, :questionId, :selectedOptionKey, NOW())
            ON CONFLICT (attempt_id, question_id)
            DO UPDATE SET
                selected_option_key = EXCLUDED.selected_option_key,
                answered_at         = EXCLUDED.answered_at
            """, nativeQuery = true)
    void upsertAnswer(
            @Param("attemptId") Long attemptId,
            @Param("questionId") Long questionId,
            @Param("selectedOptionKey") String selectedOptionKey
    );
}
