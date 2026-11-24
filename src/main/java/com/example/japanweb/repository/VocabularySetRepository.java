package com.example.japanweb.repository;

import com.example.japanweb.entity.VocabularySet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VocabularySetRepository extends JpaRepository<VocabularySet, String> {
    List<VocabularySet> findByOwnerIdOrderByUpdatedAtDescIdAsc(Long ownerId);
    List<VocabularySet> findByCommunityTrueOrderByUpdatedAtDescIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from VocabularySet s where s.id = :id")
    Optional<VocabularySet> lockById(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from VocabularySet s where s.id = :id")
    Optional<VocabularySet> lockForRead(@Param("id") String id);
}
