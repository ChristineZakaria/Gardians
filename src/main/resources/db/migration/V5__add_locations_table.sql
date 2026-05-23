-- ============================================================
-- V5: Add locations table (persists latest GPS per device)
-- ============================================================

CREATE TABLE IF NOT EXISTS locations (
    device_id         VARCHAR(100)     PRIMARY KEY,
    device_name       VARCHAR(150),
    latitude          DOUBLE PRECISION NOT NULL,
    longitude         DOUBLE PRECISION NOT NULL,
    linked_parent_id  BIGINT,
    updated_at        TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_locations_parent ON locations(linked_parent_id);
