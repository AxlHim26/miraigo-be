package com.example.japanweb.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vocab_entries", indexes = {
        @Index(name = "idx_vocab_entries_course_term", columnList = "course_id, term"),
        @Index(name = "idx_vocab_entries_level", columnList = "level"),
        @Index(name = "idx_vocab_entries_course_random_key", columnList = "course_id, random_key")
})
public class VocabEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VocabCourse course;

    @Column(nullable = false, length = 100)
    private String term;

    @Column(nullable = false, length = 200)
    private String reading;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meaning;

    @Column(columnDefinition = "TEXT")
    private String example;

    @Column(length = 10)
    private String level;

    @Column(name = "random_key", nullable = false)
    private Double randomKey;

    @PrePersist
    void assignRandomKey() {
        if (randomKey == null) {
            randomKey = ThreadLocalRandom.current().nextDouble();
        }
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
