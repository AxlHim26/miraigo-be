CREATE TABLE IF NOT EXISTS jlpt_section_attempts (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES jlpt_attempts(id) ON DELETE CASCADE,
    section_id BIGINT NOT NULL REFERENCES jlpt_sections(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(attempt_id, section_id)
);

CREATE INDEX IF NOT EXISTS idx_jlpt_sect_attempts_status ON jlpt_section_attempts(attempt_id, status);
CREATE INDEX IF NOT EXISTS idx_jlpt_sect_attempts_expires ON jlpt_section_attempts(expires_at);
