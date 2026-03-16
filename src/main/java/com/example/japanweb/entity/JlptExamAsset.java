package com.example.japanweb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jlpt_exam_assets")
public class JlptExamAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private JlptExam exam;

    @Column(name = "asset_type", nullable = false, length = 40)
    private String assetType;

    @Column(name = "source_path", nullable = false, length = 800)
    private String sourcePath;

    @Column(name = "extracted_text_path", length = 800)
    private String extractedTextPath;

    @Column(name = "quality", length = 40)
    private String quality;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
