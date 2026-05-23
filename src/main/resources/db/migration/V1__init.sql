-- ============================================================
-- Guardians DB Schema - V1
-- ============================================================

-- USERS
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    phone           VARCHAR(20),
    full_name       VARCHAR(150)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'PARENT',
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    fcm_token       VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- DEVICES
CREATE TABLE IF NOT EXISTS devices (
    id              BIGSERIAL       PRIMARY KEY,
    device_id       VARCHAR(100)    NOT NULL UNIQUE,
    device_name     VARCHAR(150),
    type            VARCHAR(20)     NOT NULL DEFAULT 'CHILD',
    owner_user_id   BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    linked_parent_id BIGINT         REFERENCES users(id) ON DELETE SET NULL,
    fcm_token       VARCHAR(512),
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    last_seen       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- PAIRING_CODES
CREATE TABLE IF NOT EXISTS pairing_codes (
    id              BIGSERIAL       PRIMARY KEY,
    code            VARCHAR(10)     NOT NULL UNIQUE,
    parent_id       BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_used         BOOLEAN         NOT NULL DEFAULT false,
    expires_at      TIMESTAMPTZ     NOT NULL,
    used_at         TIMESTAMPTZ,
    child_device_id BIGINT          REFERENCES devices(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ALERTS
CREATE TABLE IF NOT EXISTS alerts (
    id              BIGSERIAL       PRIMARY KEY,
    parent_id       BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_device_id BIGINT         REFERENCES devices(id) ON DELETE SET NULL,
    type            VARCHAR(50)     NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    message         TEXT,
    severity        VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    is_read         BOOLEAN         NOT NULL DEFAULT false,
    metadata        JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_devices_owner        ON devices(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_devices_parent       ON devices(linked_parent_id);
CREATE INDEX IF NOT EXISTS idx_pairing_code         ON pairing_codes(code) WHERE is_used = false;
CREATE INDEX IF NOT EXISTS idx_alerts_parent        ON alerts(parent_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_unread        ON alerts(parent_id, is_read) WHERE is_read = false;
