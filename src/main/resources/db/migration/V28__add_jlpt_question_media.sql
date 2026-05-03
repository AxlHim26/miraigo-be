-- V21__add_jlpt_question_media.sql
-- Add passage text and audio url for reading/listening questions
ALTER TABLE jlpt_questions ADD COLUMN passage_text TEXT;
ALTER TABLE jlpt_questions ADD COLUMN audio_url VARCHAR(800);
