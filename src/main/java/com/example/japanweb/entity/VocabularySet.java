package com.example.japanweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "vocabulary_sets", indexes = {
    @Index(name = "ix_vocabulary_sets_owner", columnList = "owner_id"),
    @Index(name = "ix_vocabulary_sets_community", columnList = "community, updated_at")
})
@Getter
@Setter
@NoArgsConstructor
public class VocabularySet {
    @Id
    @Column(length = 128)
    private String id;

    @Version
    private Long version;

    // Imported catalog sets have no owner. Only community sets are readable without an owner.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 2)
    private String level;

    @Column(nullable = false)
    private boolean community;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
