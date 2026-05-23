-- Call log: stores last N calls per device (replaced on each sync)
CREATE TABLE IF NOT EXISTS call_logs (
    id               BIGSERIAL PRIMARY KEY,
    device_id        VARCHAR(100)  NOT NULL,
    number           VARCHAR(50)   NOT NULL DEFAULT '',
    name             VARCHAR(255)  NOT NULL DEFAULT '',
    call_type        VARCHAR(20)   NOT NULL DEFAULT 'UNKNOWN',
    duration_seconds INT           NOT NULL DEFAULT 0,
    timestamp        BIGINT        NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_call_logs_device ON call_logs (device_id);

-- SMS log: stores last N messages per device (replaced on each sync)
CREATE TABLE IF NOT EXISTS sms_logs (
    id         BIGSERIAL PRIMARY KEY,
    device_id  VARCHAR(100)  NOT NULL,
    address    VARCHAR(255)  NOT NULL DEFAULT '',
    body       TEXT          NOT NULL DEFAULT '',
    sms_type   VARCHAR(20)   NOT NULL DEFAULT 'OTHER',
    timestamp  BIGINT        NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_sms_logs_device ON sms_logs (device_id);

-- Per-device settings (one row per device, upserted)
CREATE TABLE IF NOT EXISTS device_settings (
    device_id                 VARCHAR(100) PRIMARY KEY,
    screen_time_limit_minutes INT          NOT NULL DEFAULT 0,
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
