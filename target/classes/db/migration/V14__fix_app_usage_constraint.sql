-- V13 tried to drop 'app_usage_device_id_package_name_key' but the actual
-- constraint name from V3 is 'uq_app_usage_device_pkg'. Drop it now so
-- the per-day unique key (device_id, package_name, date) is the only one.
ALTER TABLE app_usage DROP CONSTRAINT IF EXISTS uq_app_usage_device_pkg;
