-- Add expires_at column to jlpt_attempts
ALTER TABLE jlpt_attempts ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

-- Create index on expires_at for fast lookup of expired attempts
CREATE INDEX IF NOT EXISTS idx_jlpt_attempts_expires_at ON jlpt_attempts(expires_at);

-- Create a batch upsert function for performance
CREATE OR REPLACE FUNCTION batch_upsert_jlpt_attempt_answers(
    p_attempt_id BIGINT,
    p_question_ids BIGINT[],
    p_selected_options VARCHAR[]
) RETURNS VOID AS $$
BEGIN
    INSERT INTO jlpt_attempt_answers (attempt_id, question_id, selected_option_key, answered_at)
    SELECT p_attempt_id, unnest(p_question_ids), unnest(p_selected_options), CURRENT_TIMESTAMP
    ON CONFLICT (attempt_id, question_id) 
    DO UPDATE SET 
        selected_option_key = EXCLUDED.selected_option_key,
        answered_at = EXCLUDED.answered_at;
END;
$$ LANGUAGE plpgsql;
