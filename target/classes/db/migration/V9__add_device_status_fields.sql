ALTER TABLE devices
    ADD COLUMN IF NOT EXISTS battery_level    INT     DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_charging      BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS current_app      VARCHAR(150);
