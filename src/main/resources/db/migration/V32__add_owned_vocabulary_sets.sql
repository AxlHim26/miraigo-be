CREATE TABLE IF NOT EXISTS vocabulary_sets (
    id VARCHAR(128) PRIMARY KEY,
    version BIGINT NOT NULL,
    owner_id BIGINT REFERENCES users (id),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    level VARCHAR(2),
    community BOOLEAN NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_vocabulary_sets_owner ON vocabulary_sets (owner_id);
CREATE INDEX IF NOT EXISTS ix_vocabulary_sets_community ON vocabulary_sets (community, updated_at);

CREATE TABLE IF NOT EXISTS vocabulary_set_words (
    set_id VARCHAR(128) NOT NULL REFERENCES vocabulary_sets (id),
    word_id VARCHAR(128) NOT NULL,
    japanese VARCHAR(200) NOT NULL,
    reading VARCHAR(200) NOT NULL,
    meaning VARCHAR(1000) NOT NULL,
    example VARCHAR(2000),
    example_meaning VARCHAR(2000),
    PRIMARY KEY (set_id, word_id)
);

CREATE INDEX IF NOT EXISTS ix_vocabulary_words_set ON vocabulary_set_words (set_id);
