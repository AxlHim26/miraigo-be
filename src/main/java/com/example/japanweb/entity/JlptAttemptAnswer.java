package com.example.japanweb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
// The answer upsert relies on this constraint (ON CONFLICT). The tables are created by Hibernate, so it has to be
// declared here; the name matches the one PostgreSQL gives the constraint in migration V22.
@Table(name = "jlpt_attempt_answers", uniqueConstraints = @UniqueConstraint(
        name = "jlpt_attempt_answers_attempt_id_question_id_key",
        columnNames = {"attempt_id", "question_id"}))
public class JlptAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private JlptAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private JlptQuestion question;

    @Column(name = "selected_option_key", length = 2)
    private String selectedOptionKey;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}
