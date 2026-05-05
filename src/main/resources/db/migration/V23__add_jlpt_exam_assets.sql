CREATE TABLE IF NOT EXISTS jlpt_exam_assets (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES jlpt_exams(id) ON DELETE CASCADE,
    asset_type VARCHAR(40) NOT NULL,
    source_path VARCHAR(800) NOT NULL,
    extracted_text_path VARCHAR(800),
    quality VARCHAR(40),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(exam_id, asset_type, source_path)
);

CREATE INDEX IF NOT EXISTS idx_jlpt_exam_assets_exam ON jlpt_exam_assets(exam_id);
CREATE INDEX IF NOT EXISTS idx_jlpt_exam_assets_type ON jlpt_exam_assets(asset_type);
