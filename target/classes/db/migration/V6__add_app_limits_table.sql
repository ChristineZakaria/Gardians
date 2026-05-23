-- ============================================================
-- V6: Add app_limits table (per-app daily time limits set by parent)
-- ============================================================

CREATE TABLE IF NOT EXISTS app_limits (
    id              BIGSERIAL       PRIMARY KEY,
    device_id       VARCHAR(100)    NOT NULL,
    package_name    VARCHAR(255)    NOT NULL,
    limit_minutes   INT             NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_limit_device_pkg UNIQUE (device_id, package_name)
);

CREATE INDEX IF NOT EXISTS idx_app_limits_device ON app_limits(device_id);
