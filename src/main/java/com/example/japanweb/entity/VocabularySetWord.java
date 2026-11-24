package com.example.japanweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vocabulary_set_words", indexes = {
    @Index(name = "ix_vocabulary_words_set", columnList = "set_id")
})
@Getter
@Setter
@NoArgsConstructor
public class VocabularySetWord {
    @EmbeddedId
    private VocabularySetWordId id;

    @MapsId("setId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private VocabularySet vocabularySet;

    @Column(nullable = false, length = 200)
    private String japanese;

    @Column(nullable = false, length = 200)
    private String reading;

    @Column(nullable = false, length = 1000)
    private String meaning;

    @Column(length = 2000)
    private String example;

    @Column(length = 2000)
    private String exampleMeaning;
}
