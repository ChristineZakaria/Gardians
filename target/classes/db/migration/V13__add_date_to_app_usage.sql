ALTER TABLE app_usage ADD COLUMN IF NOT EXISTS date DATE NOT NULL DEFAULT CURRENT_DATE;
ALTER TABLE app_usage DROP CONSTRAINT IF EXISTS app_usage_device_id_package_name_key;
ALTER TABLE app_usage ADD CONSTRAINT app_usage_device_package_date_key UNIQUE (device_id, package_name, date);
CREATE INDEX IF NOT EXISTS idx_app_usage_device_date ON app_usage(device_id, date);
