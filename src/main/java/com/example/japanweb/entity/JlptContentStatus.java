package com.example.japanweb.entity;

/**
 * Content completeness of a JLPT exam.
 *
 * <ul>
 *   <li>{@code DRAFT}    – questions are partially or not yet imported; exam must not be served to users.</li>
 *   <li>{@code COMPLETE} – all questions, options, and answer keys have been imported and verified.</li>
 * </ul>
 *
 * The database column {@code jlpt_exams.content_status} carries a matching CHECK constraint
 * (added in V18) that rejects any value outside this set.
 */
public enum JlptContentStatus {
    DRAFT,
    COMPLETE
}
