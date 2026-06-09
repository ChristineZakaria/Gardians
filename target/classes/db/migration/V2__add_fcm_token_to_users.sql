-- Add fcm_token column to users table if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512);
