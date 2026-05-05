-- V30__update_grammar_examples_schema.sql
-- Update grammar_examples schema to match JPA entity properties

ALTER TABLE grammar_examples RENAME COLUMN japanese TO sentence_jp;
ALTER TABLE grammar_examples RENAME COLUMN meaning TO sentence_vn;

ALTER TABLE grammar_examples DROP COLUMN IF EXISTS reading;
ALTER TABLE grammar_examples DROP COLUMN IF EXISTS display_order;

ALTER TABLE grammar_examples ADD COLUMN IF NOT EXISTS audio_url VARCHAR(500);
