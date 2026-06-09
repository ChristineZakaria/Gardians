CREATE TABLE IF NOT EXISTS verifications (
    id               BIGSERIAL       PRIMARY KEY,
    user_id          BIGINT          NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    role_type        VARCHAR(20),
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    face_match_score DOUBLE PRECISION,
    name_match       BOOLEAN,
    child_name       VARCHAR(150),
    reject_reason    VARCHAR(500),
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    reviewed_at      TIMESTAMPTZ
);
