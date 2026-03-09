-- Fix G: Prevent two IN_PROGRESS attempts for the same user + exam simultaneously.
-- A partial unique index is used so that SUBMITTED attempts are not affected
-- (a user may have multiple completed attempts for the same exam).
CREATE UNIQUE INDEX IF NOT EXISTS idx_jlpt_attempts_one_inprogress_per_user_exam
    ON jlpt_attempts (exam_id, user_id)
    WHERE status = 'IN_PROGRESS';

-- Fix I: Enforce the two legal values for content_status at the DB level so
-- that a typo in the application layer is rejected immediately rather than
-- silently stored and later causing exams to disappear from the public list.
ALTER TABLE jlpt_exams
    ADD CONSTRAINT chk_jlpt_exams_content_status
    CHECK (content_status IN ('DRAFT', 'COMPLETE'));
