package com.example.japanweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record VocabularySetWordId(
    @Column(name = "set_id", length = 128) String setId,
    @Column(name = "word_id", length = 128) String wordId
) implements Serializable {}
