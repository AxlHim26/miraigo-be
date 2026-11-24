package com.example.japanweb.repository;

import com.example.japanweb.dto.response.vocab.VocabProgressDTO;
import com.example.japanweb.entity.UserLearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLearningProgressRepository extends JpaRepository<UserLearningProgress, Long> {

    /**
     * Find progress for a specific user and vocabulary entry
     */
    Optional<UserLearningProgress> findByUserIdAndVocabId(Long userId, Long vocabId);

    /**
     * Find all vocabulary entries due for review for a user
     */
    @Query("SELECT ulp FROM UserLearningProgress ulp " +
            "WHERE ulp.user.id = :userId AND ulp.nextReviewAt <= :now " +
            "ORDER BY ulp.nextReviewAt ASC")
    List<UserLearningProgress> findDueForReview(@Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    /**
     * Find vocabulary entries due for review with limit
     */
    @Query(value = "SELECT * FROM user_learning_progress " +
            "WHERE user_id = :userId AND next_review_at <= :now " +
            "ORDER BY next_review_at ASC LIMIT :limit", nativeQuery = true)
    List<UserLearningProgress> findDueForReviewWithLimit(@Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    /**
     * Count total vocabulary entries a user has started learning
     */
    long countByUserId(Long userId);

    @Query("""
            SELECT new com.example.japanweb.dto.response.vocab.VocabProgressDTO(
                progress.vocab.id, progress.vocab.course.id, progress.createdAt, progress.lastReviewedAt,
                progress.nextReviewAt, progress.repetitions)
            FROM UserLearningProgress progress
            WHERE progress.user.id = :userId
            """)
    List<VocabProgressDTO> findProgressByUserId(@Param("userId") Long userId);

    /**
     * Count vocabulary entries due for review
     */
    @Query("SELECT COUNT(ulp) FROM UserLearningProgress ulp " +
            "WHERE ulp.user.id = :userId AND ulp.nextReviewAt <= :now")
    long countDueForReview(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Find all progress entries for a user in a specific course
     */
    @Query("SELECT ulp FROM UserLearningProgress ulp " +
            "WHERE ulp.user.id = :userId AND ulp.vocab.course.id = :courseId")
    List<UserLearningProgress> findByUserIdAndCourseId(@Param("userId") Long userId,
            @Param("courseId") Long courseId);
}
