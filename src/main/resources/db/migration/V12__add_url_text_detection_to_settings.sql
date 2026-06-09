ALTER TABLE device_settings ADD COLUMN IF NOT EXISTS url_detection_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE device_settings ADD COLUMN IF NOT EXISTS text_detection_enabled BOOLEAN NOT NULL DEFAULT true;
