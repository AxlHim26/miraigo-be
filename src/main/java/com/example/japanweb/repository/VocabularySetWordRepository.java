package com.example.japanweb.repository;

import com.example.japanweb.entity.VocabularySetWord;
import com.example.japanweb.entity.VocabularySetWordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VocabularySetWordRepository extends JpaRepository<VocabularySetWord, VocabularySetWordId> {
    interface WordCount {
        String getSetId();
        long getTotal();
    }

    List<VocabularySetWord> findByIdSetIdOrderByIdWordIdAsc(String setId);
    long countByIdSetId(String setId);

    @Query("select w.id.setId as setId, count(w) as total from VocabularySetWord w where w.id.setId in :ids group by w.id.setId")
    List<WordCount> countBySetIds(@Param("ids") Collection<String> ids);

    @Modifying
    @Query("delete from VocabularySetWord w where w.id.setId = :id")
    void deleteForSet(@Param("id") String id);
}
