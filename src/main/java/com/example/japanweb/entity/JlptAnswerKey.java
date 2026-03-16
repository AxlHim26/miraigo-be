package com.example.japanweb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jlpt_answer_keys")
public class JlptAnswerKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private JlptQuestion question;

    @Column(name = "correct_option_key", nullable = false, length = 2)
    private String correctOptionKey;

    @Column(name = "score_weight", nullable = false)
    private Integer scoreWeight;
}
