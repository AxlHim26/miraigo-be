package com.example.japanweb.repository;

import com.example.japanweb.entity.JlptExamAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JlptExamAssetRepository extends JpaRepository<JlptExamAsset, Long> {

    void deleteByExamIdIn(Collection<Long> examIds);

    List<JlptExamAsset> findByExamId(Long examId);
}
