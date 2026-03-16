ALTER TABLE jlpt_exams
    ADD COLUMN IF NOT EXISTS content_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

CREATE INDEX IF NOT EXISTS idx_jlpt_exams_content_status ON jlpt_exams(content_status);

-- The existing placeholder exam (N4-2023-07 with sample questions from V15) stays as DRAFT
-- until the real questions are imported via the import-parsed-exam endpoint.
