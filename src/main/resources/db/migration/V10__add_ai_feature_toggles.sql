ALTER TABLE device_settings
    ADD COLUMN IF NOT EXISTS image_detection_enabled  BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS video_detection_enabled  BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS content_scan_enabled     BOOLEAN NOT NULL DEFAULT true;
