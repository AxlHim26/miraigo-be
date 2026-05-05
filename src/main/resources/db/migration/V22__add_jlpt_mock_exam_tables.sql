CREATE TABLE IF NOT EXISTS jlpt_exams (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    level VARCHAR(10) NOT NULL,
    exam_year INTEGER NOT NULL,
    exam_month INTEGER NOT NULL,
    total_duration_minutes INTEGER NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_jlpt_exams_level ON jlpt_exams(level);
CREATE INDEX IF NOT EXISTS idx_jlpt_exams_schedule ON jlpt_exams(exam_year DESC, exam_month DESC);

CREATE TABLE IF NOT EXISTS jlpt_sections (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES jlpt_exams(id) ON DELETE CASCADE,
    section_type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    section_order INTEGER NOT NULL,
    duration_minutes INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(exam_id, section_order)
);

CREATE INDEX IF NOT EXISTS idx_jlpt_sections_exam ON jlpt_sections(exam_id, section_order);

CREATE TABLE IF NOT EXISTS jlpt_questions (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL REFERENCES jlpt_sections(id) ON DELETE CASCADE,
    part_number INTEGER,
    question_number INTEGER NOT NULL,
    prompt TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(section_id, question_number)
);

CREATE INDEX IF NOT EXISTS idx_jlpt_questions_section ON jlpt_questions(section_id, question_number);

CREATE TABLE IF NOT EXISTS jlpt_question_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES jlpt_questions(id) ON DELETE CASCADE,
    option_key VARCHAR(2) NOT NULL,
    option_text TEXT NOT NULL,
    option_order INTEGER NOT NULL,
    UNIQUE(question_id, option_key)
);

CREATE INDEX IF NOT EXISTS idx_jlpt_options_question ON jlpt_question_options(question_id, option_order);

CREATE TABLE IF NOT EXISTS jlpt_answer_keys (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL UNIQUE REFERENCES jlpt_questions(id) ON DELETE CASCADE,
    correct_option_key VARCHAR(2) NOT NULL,
    score_weight INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS jlpt_attempts (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES jlpt_exams(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP,
    total_scaled_score INTEGER,
    passed BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_jlpt_attempts_user ON jlpt_attempts(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_jlpt_attempts_status ON jlpt_attempts(status);

CREATE TABLE IF NOT EXISTS jlpt_attempt_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES jlpt_attempts(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES jlpt_questions(id) ON DELETE CASCADE,
    selected_option_key VARCHAR(2),
    answered_at TIMESTAMP,
    UNIQUE(attempt_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_jlpt_attempt_answers_attempt ON jlpt_attempt_answers(attempt_id);

INSERT INTO jlpt_exams (id, code, title, level, exam_year, exam_month, total_duration_minutes, is_published)
VALUES (1001, 'N4-2023-07', 'JLPT N4 - 2023.07 Mock', 'N4', 2023, 7, 125, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO jlpt_sections (id, exam_id, section_type, title, section_order, duration_minutes)
VALUES
    (2001, 1001, 'LANGUAGE_KNOWLEDGE', '言語知識（文字・語彙・文法）', 1, 30),
    (2002, 1001, 'READING', '読解', 2, 35),
    (2003, 1001, 'LISTENING', '聴解', 3, 28)
ON CONFLICT (id) DO NOTHING;

INSERT INTO jlpt_questions (id, section_id, part_number, question_number, prompt)
VALUES
    (3001, 2001, 1, 1, '「最近、とても忙しいです。」の「最近」はどれですか。'),
    (3002, 2001, 1, 2, '「少しお金が足りませんでした。」の「足りません」はどれですか。'),
    (3003, 2002, 1, 3, '文章を読んで、筆者の意見として最も近いものを選んでください。'),
    (3004, 2002, 1, 4, '次の文脈で正しい接続を選んでください。'),
    (3005, 2003, 1, 5, '会話を聞いて、男の人がまず何をしますか。'),
    (3006, 2003, 1, 6, 'アナウンスを聞いて、正しい情報を選んでください。')
ON CONFLICT (id) DO NOTHING;

INSERT INTO jlpt_question_options (question_id, option_key, option_text, option_order)
VALUES
    (3001, 'A', 'さいしん', 1),
    (3001, 'B', 'すいきん', 2),
    (3001, 'C', 'さいきん', 3),
    (3001, 'D', 'すいしん', 4),

    (3002, 'A', 'あしりません', 1),
    (3002, 'B', 'たりません', 2),
    (3002, 'C', 'だりません', 3),
    (3002, 'D', 'あじりません', 4),

    (3003, 'A', '写真を撮ることが好きだ。', 1),
    (3003, 'B', '忙しいので旅行しない。', 2),
    (3003, 'C', '毎日山へ行っている。', 3),
    (3003, 'D', '海には行ったことがない。', 4),

    (3004, 'A', 'それで', 1),
    (3004, 'B', 'しかし', 2),
    (3004, 'C', 'つまり', 3),
    (3004, 'D', 'そして', 4),

    (3005, 'A', '工場へ行く', 1),
    (3005, 'B', '山下食品にメールする', 2),
    (3005, 'C', '先に工場に納期確認する', 3),
    (3005, 'D', '契約書を作り直す', 4),

    (3006, 'A', '集合時間は8時です。', 1),
    (3006, 'B', '会場は駅前ホールです。', 2),
    (3006, 'C', '参加費は無料です。', 3),
    (3006, 'D', '申し込みは不要です。', 4)
ON CONFLICT (question_id, option_key) DO NOTHING;

INSERT INTO jlpt_answer_keys (question_id, correct_option_key, score_weight)
VALUES
    (3001, 'C', 1),
    (3002, 'B', 1),
    (3003, 'A', 1),
    (3004, 'B', 1),
    (3005, 'C', 1),
    (3006, 'C', 1)
ON CONFLICT (question_id) DO NOTHING;

SELECT setval('jlpt_exams_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM jlpt_exams), 1001));
SELECT setval('jlpt_sections_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM jlpt_sections), 2003));
SELECT setval('jlpt_questions_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM jlpt_questions), 3006));
