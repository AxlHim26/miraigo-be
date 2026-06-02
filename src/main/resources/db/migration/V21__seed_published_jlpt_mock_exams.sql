-- Seed visible JLPT mock exams for the learner frontend.
-- The public list API only returns COMPLETE exams, so every seeded mock exam
-- must be published and complete.

UPDATE jlpt_exams
SET is_published = TRUE,
    content_status = 'COMPLETE'
WHERE code = 'N4-2023-07';

INSERT INTO jlpt_exams (code, title, level, exam_year, exam_month, total_duration_minutes, is_published, content_status)
VALUES
    ('N5-MOCK-1', 'JLPT N5 Mock Test 1', 'N5', 2026, 1, 90, TRUE, 'COMPLETE'),
    ('N4-MOCK-1', 'JLPT N4 Mock Test 1', 'N4', 2026, 1, 125, TRUE, 'COMPLETE'),
    ('N3-MOCK-1', 'JLPT N3 Mock Test 1', 'N3', 2026, 1, 140, TRUE, 'COMPLETE'),
    ('N2-MOCK-1', 'JLPT N2 Mock Test 1', 'N2', 2026, 1, 155, TRUE, 'COMPLETE'),
    ('N1-MOCK-1', 'JLPT N1 Mock Test 1', 'N1', 2026, 1, 170, TRUE, 'COMPLETE')
ON CONFLICT (code) DO UPDATE
SET title = EXCLUDED.title,
    level = EXCLUDED.level,
    exam_year = EXCLUDED.exam_year,
    exam_month = EXCLUDED.exam_month,
    total_duration_minutes = EXCLUDED.total_duration_minutes,
    is_published = TRUE,
    content_status = 'COMPLETE';

WITH seeded_sections AS (
    SELECT
        e.id AS exam_id,
        s.section_type,
        s.title,
        s.section_order,
        s.duration_minutes
    FROM jlpt_exams e
    JOIN (
        VALUES
            ('N5-MOCK-1', 'LANGUAGE_KNOWLEDGE', '言語知識（文字・語彙）', 1, 25),
            ('N5-MOCK-1', 'GRAMMAR_KNOWLEDGE', '言語知識（文法）', 2, 25),
            ('N5-MOCK-1', 'READING_COMPREHENSION', '読解', 3, 40),
            ('N4-MOCK-1', 'LANGUAGE_KNOWLEDGE', '言語知識（文字・語彙）', 1, 30),
            ('N4-MOCK-1', 'GRAMMAR_KNOWLEDGE', '言語知識（文法）', 2, 35),
            ('N4-MOCK-1', 'READING_COMPREHENSION', '読解', 3, 60),
            ('N3-MOCK-1', 'LANGUAGE_KNOWLEDGE', '言語知識（文字・語彙）', 1, 30),
            ('N3-MOCK-1', 'GRAMMAR_KNOWLEDGE', '言語知識（文法）', 2, 40),
            ('N3-MOCK-1', 'READING_COMPREHENSION', '読解', 3, 70),
            ('N2-MOCK-1', 'LANGUAGE_KNOWLEDGE', '言語知識（文字・語彙）', 1, 35),
            ('N2-MOCK-1', 'GRAMMAR_KNOWLEDGE', '言語知識（文法）', 2, 40),
            ('N2-MOCK-1', 'READING_COMPREHENSION', '読解', 3, 80),
            ('N1-MOCK-1', 'LANGUAGE_KNOWLEDGE', '言語知識（文字・語彙）', 1, 40),
            ('N1-MOCK-1', 'GRAMMAR_KNOWLEDGE', '言語知識（文法）', 2, 45),
            ('N1-MOCK-1', 'READING_COMPREHENSION', '読解', 3, 85)
    ) AS s(code, section_type, title, section_order, duration_minutes) ON s.code = e.code
)
INSERT INTO jlpt_sections (exam_id, section_type, title, section_order, duration_minutes)
SELECT exam_id, section_type, title, section_order, duration_minutes
FROM seeded_sections
ON CONFLICT (exam_id, section_order) DO NOTHING;

WITH seeded_questions AS (
    SELECT
        e.code,
        s.id AS section_id,
        q.part_number,
        q.question_number,
        q.prompt,
        q.correct_option_key,
        q.option_1,
        q.option_2,
        q.option_3,
        q.option_4
    FROM jlpt_exams e
    JOIN jlpt_sections s ON s.exam_id = e.id
    JOIN (
        VALUES
            ('N5-MOCK-1', 1, 1, 1, 'それは（くるま）です。', '1', '車', '東', '重', '里'),
            ('N5-MOCK-1', 2, 2, 1, 'きのうは とても ___ です。', '2', 'さむい', 'さむかった', 'さむくない', 'さむくなかった'),
            ('N5-MOCK-1', 3, 3, 1, '短い文章を読んで、正しい答えを選んでください。毎朝、山田さんは七時に起きます。朝ごはんを食べて、学校へ行きます。山田さんは何時に起きますか。', '2', '六時', '七時', '八時', '九時'),
            ('N4-MOCK-1', 1, 1, 1, '「最近」はどの読み方ですか。', '2', 'さいしん', 'さいきん', 'すいきん', 'すいしん'),
            ('N4-MOCK-1', 2, 2, 1, '雨が降っています。___、傘を持って行きます。', '2', 'でも', 'それで', 'しかし', 'ところで'),
            ('N4-MOCK-1', 3, 3, 1, 'メールを読んで、正しい答えを選んでください。明日の会議は午後三時からです。会議はいつですか。', '4', '今日の午前三時', '今日の午後三時', '明日の午前三時', '明日の午後三時'),
            ('N3-MOCK-1', 1, 1, 1, '「確認」の意味として最も近いものを選んでください。', '1', 'たしかめること', 'わすれること', 'やめること', 'こわすこと'),
            ('N3-MOCK-1', 2, 2, 1, 'この資料は大切なので、なくさない ___ してください。', '1', 'ように', 'ために', 'ところに', 'ばかりに'),
            ('N3-MOCK-1', 3, 3, 1, '文章を読んで、筆者の考えに最も近いものを選んでください。新しい習慣を続けるには、毎日少しずつ行うことが大切です。', '2', '一度に大きく変えるべきだ', '毎日少しずつ続けるとよい', '習慣は必要ない', '朝だけ勉強すればよい'),
            ('N2-MOCK-1', 1, 1, 1, '「需要」の意味として最も近いものを選んでください。', '1', 'ものを求める量', '会社の住所', '古い書類', '短い休み'),
            ('N2-MOCK-1', 2, 2, 1, '彼は忙しい ___、いつも手伝ってくれます。', '3', 'ながら', 'ために', 'にもかかわらず', 'にしたがって'),
            ('N2-MOCK-1', 3, 3, 1, '文章を読んで、内容に合うものを選んでください。近年、在宅勤務を導入する企業が増えている。', '2', '在宅勤務をやめる企業が増えている', '在宅勤務を導入する企業が増えている', '企業は勤務制度を変えていない', '在宅勤務は法律で禁止された'),
            ('N1-MOCK-1', 1, 1, 1, '「矛盾」の意味として最も近いものを選んでください。', '1', '言っていることが食い違うこと', '強く同意すること', '静かに待つこと', '細かく記録すること'),
            ('N1-MOCK-1', 2, 2, 1, '結果のいかん ___、計画は見直す必要がある。', '1', 'を問わず', 'としても', 'だけに', 'に沿って'),
            ('N1-MOCK-1', 3, 3, 1, '文章を読んで、要旨として最も適切なものを選んでください。技術の進歩は便利さをもたらす一方で、新しい課題も生み出している。', '2', '技術は課題を完全になくす', '技術は便利だが課題も生む', '技術は進歩していない', '便利さは不要である')
    ) AS q(code, section_order, part_number, question_number, prompt, correct_option_key, option_1, option_2, option_3, option_4)
        ON q.code = e.code AND q.section_order = s.section_order
)
INSERT INTO jlpt_questions (section_id, part_number, question_number, prompt)
SELECT section_id, part_number, question_number, prompt
FROM seeded_questions
ON CONFLICT (section_id, question_number) DO NOTHING;

WITH seeded_options AS (
    SELECT
        jq.id AS question_id,
        opt.option_key,
        opt.option_text,
        opt.option_order
    FROM jlpt_exams e
    JOIN jlpt_sections s ON s.exam_id = e.id
    JOIN jlpt_questions jq ON jq.section_id = s.id
    JOIN (
        VALUES
            ('N5-MOCK-1', 1, 1, '1', '車', 1), ('N5-MOCK-1', 1, 1, '2', '東', 2), ('N5-MOCK-1', 1, 1, '3', '重', 3), ('N5-MOCK-1', 1, 1, '4', '里', 4),
            ('N5-MOCK-1', 2, 1, '1', 'さむい', 1), ('N5-MOCK-1', 2, 1, '2', 'さむかった', 2), ('N5-MOCK-1', 2, 1, '3', 'さむくない', 3), ('N5-MOCK-1', 2, 1, '4', 'さむくなかった', 4),
            ('N5-MOCK-1', 3, 1, '1', '六時', 1), ('N5-MOCK-1', 3, 1, '2', '七時', 2), ('N5-MOCK-1', 3, 1, '3', '八時', 3), ('N5-MOCK-1', 3, 1, '4', '九時', 4),
            ('N4-MOCK-1', 1, 1, '1', 'さいしん', 1), ('N4-MOCK-1', 1, 1, '2', 'さいきん', 2), ('N4-MOCK-1', 1, 1, '3', 'すいきん', 3), ('N4-MOCK-1', 1, 1, '4', 'すいしん', 4),
            ('N4-MOCK-1', 2, 1, '1', 'でも', 1), ('N4-MOCK-1', 2, 1, '2', 'それで', 2), ('N4-MOCK-1', 2, 1, '3', 'しかし', 3), ('N4-MOCK-1', 2, 1, '4', 'ところで', 4),
            ('N4-MOCK-1', 3, 1, '1', '今日の午前三時', 1), ('N4-MOCK-1', 3, 1, '2', '今日の午後三時', 2), ('N4-MOCK-1', 3, 1, '3', '明日の午前三時', 3), ('N4-MOCK-1', 3, 1, '4', '明日の午後三時', 4),
            ('N3-MOCK-1', 1, 1, '1', 'たしかめること', 1), ('N3-MOCK-1', 1, 1, '2', 'わすれること', 2), ('N3-MOCK-1', 1, 1, '3', 'やめること', 3), ('N3-MOCK-1', 1, 1, '4', 'こわすこと', 4),
            ('N3-MOCK-1', 2, 1, '1', 'ように', 1), ('N3-MOCK-1', 2, 1, '2', 'ために', 2), ('N3-MOCK-1', 2, 1, '3', 'ところに', 3), ('N3-MOCK-1', 2, 1, '4', 'ばかりに', 4),
            ('N3-MOCK-1', 3, 1, '1', '一度に大きく変えるべきだ', 1), ('N3-MOCK-1', 3, 1, '2', '毎日少しずつ続けるとよい', 2), ('N3-MOCK-1', 3, 1, '3', '習慣は必要ない', 3), ('N3-MOCK-1', 3, 1, '4', '朝だけ勉強すればよい', 4),
            ('N2-MOCK-1', 1, 1, '1', 'ものを求める量', 1), ('N2-MOCK-1', 1, 1, '2', '会社の住所', 2), ('N2-MOCK-1', 1, 1, '3', '古い書類', 3), ('N2-MOCK-1', 1, 1, '4', '短い休み', 4),
            ('N2-MOCK-1', 2, 1, '1', 'ながら', 1), ('N2-MOCK-1', 2, 1, '2', 'ために', 2), ('N2-MOCK-1', 2, 1, '3', 'にもかかわらず', 3), ('N2-MOCK-1', 2, 1, '4', 'にしたがって', 4),
            ('N2-MOCK-1', 3, 1, '1', '在宅勤務をやめる企業が増えている', 1), ('N2-MOCK-1', 3, 1, '2', '在宅勤務を導入する企業が増えている', 2), ('N2-MOCK-1', 3, 1, '3', '企業は勤務制度を変えていない', 3), ('N2-MOCK-1', 3, 1, '4', '在宅勤務は法律で禁止された', 4),
            ('N1-MOCK-1', 1, 1, '1', '言っていることが食い違うこと', 1), ('N1-MOCK-1', 1, 1, '2', '強く同意すること', 2), ('N1-MOCK-1', 1, 1, '3', '静かに待つこと', 3), ('N1-MOCK-1', 1, 1, '4', '細かく記録すること', 4),
            ('N1-MOCK-1', 2, 1, '1', 'を問わず', 1), ('N1-MOCK-1', 2, 1, '2', 'としても', 2), ('N1-MOCK-1', 2, 1, '3', 'だけに', 3), ('N1-MOCK-1', 2, 1, '4', 'に沿って', 4),
            ('N1-MOCK-1', 3, 1, '1', '技術は課題を完全になくす', 1), ('N1-MOCK-1', 3, 1, '2', '技術は便利だが課題も生む', 2), ('N1-MOCK-1', 3, 1, '3', '技術は進歩していない', 3), ('N1-MOCK-1', 3, 1, '4', '便利さは不要である', 4)
    ) AS opt(code, section_order, question_number, option_key, option_text, option_order)
        ON opt.code = e.code
        AND opt.section_order = s.section_order
        AND opt.question_number = jq.question_number
)
INSERT INTO jlpt_question_options (question_id, option_key, option_text, option_order)
SELECT question_id, option_key, option_text, option_order
FROM seeded_options
ON CONFLICT (question_id, option_key) DO NOTHING;

WITH seeded_answers AS (
    SELECT
        jq.id AS question_id,
        a.correct_option_key
    FROM jlpt_exams e
    JOIN jlpt_sections s ON s.exam_id = e.id
    JOIN jlpt_questions jq ON jq.section_id = s.id
    JOIN (
        VALUES
            ('N5-MOCK-1', 1, 1, '1'), ('N5-MOCK-1', 2, 1, '2'), ('N5-MOCK-1', 3, 1, '2'),
            ('N4-MOCK-1', 1, 1, '2'), ('N4-MOCK-1', 2, 1, '2'), ('N4-MOCK-1', 3, 1, '4'),
            ('N3-MOCK-1', 1, 1, '1'), ('N3-MOCK-1', 2, 1, '1'), ('N3-MOCK-1', 3, 1, '2'),
            ('N2-MOCK-1', 1, 1, '1'), ('N2-MOCK-1', 2, 1, '3'), ('N2-MOCK-1', 3, 1, '2'),
            ('N1-MOCK-1', 1, 1, '1'), ('N1-MOCK-1', 2, 1, '1'), ('N1-MOCK-1', 3, 1, '2')
    ) AS a(code, section_order, question_number, correct_option_key)
        ON a.code = e.code
        AND a.section_order = s.section_order
        AND a.question_number = jq.question_number
)
INSERT INTO jlpt_answer_keys (question_id, correct_option_key, score_weight)
SELECT question_id, correct_option_key, 1
FROM seeded_answers
ON CONFLICT (question_id) DO NOTHING;
