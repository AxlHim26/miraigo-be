-- V20__add_jlpt_question_explanation.sql
-- Add explanation column to jlpt_questions table

ALTER TABLE jlpt_questions
ADD COLUMN explanation TEXT;
