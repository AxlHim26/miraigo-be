-- Rename existing READING sections to READING_COMPREHENSION
UPDATE jlpt_sections SET section_type = 'READING_COMPREHENSION' WHERE section_type = 'READING';

-- For exam 1001 (Mock exam), add a GRAMMAR_KNOWLEDGE section if it does not exist
INSERT INTO jlpt_sections (id, exam_id, section_type, title, section_order, duration_minutes)
VALUES (2004, 1001, 'GRAMMAR_KNOWLEDGE', '文法知識', 4, 30)
ON CONFLICT (id) DO NOTHING;

-- Update the sequence
SELECT setval('jlpt_sections_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM jlpt_sections), 2004));
