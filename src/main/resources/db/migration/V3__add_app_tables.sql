-- ============================================================
-- V3: Add blocked column to devices + app tracking tables
-- ============================================================

-- Add blocked column to devices (in case it was not created by a previous migration)
ALTER TABLE devices ADD COLUMN IF NOT EXISTS blocked BOOLEAN NOT NULL DEFAULT false;

-- APP USAGE: latest usage snapshot per device per app
CREATE TABLE IF NOT EXISTS app_usage (
    id              BIGSERIAL       PRIMARY KEY,
    device_id       VARCHAR(100)    NOT NULL,
    package_name    VARCHAR(255)    NOT NULL,
    app_name        VARCHAR(255),
    usage_millis    BIGINT          NOT NULL DEFAULT 0,
    reported_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_usage_device_pkg UNIQUE (device_id, package_name)
);

-- INSTALLED APPS: full list of installed apps per device
CREATE TABLE IF NOT EXISTS installed_apps (
    id              BIGSERIAL       PRIMARY KEY,
    device_id       VARCHAR(100)    NOT NULL,
    package_name    VARCHAR(255)    NOT NULL,
    app_name        VARCHAR(255),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_installed_device_pkg UNIQUE (device_id, package_name)
);

-- APP BLOCKS: per-app blocks per device (set by parent)
CREATE TABLE IF NOT EXISTS app_blocks (
    id              BIGSERIAL       PRIMARY KEY,
    device_id       VARCHAR(100)    NOT NULL,
    package_name    VARCHAR(255)    NOT NULL,
    blocked_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_block_device_pkg UNIQUE (device_id, package_name)
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_app_usage_device     ON app_usage(device_id);
CREATE INDEX IF NOT EXISTS idx_installed_apps_device ON installed_apps(device_id);
CREATE INDEX IF NOT EXISTS idx_app_blocks_device    ON app_blocks(device_id);
