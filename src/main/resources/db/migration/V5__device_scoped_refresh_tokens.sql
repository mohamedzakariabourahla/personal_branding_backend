ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS device_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS user_agent TEXT,
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45),
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP WITH TIME ZONE;

UPDATE refresh_tokens
SET device_id = COALESCE(device_id, md5(token_hash)),
    device_name = COALESCE(device_name, 'Unknown Device'),
    last_used_at = COALESCE(last_used_at, created_at)
WHERE device_id IS NULL
   OR last_used_at IS NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN device_id SET NOT NULL,
    ALTER COLUMN last_used_at SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS refresh_tokens_user_device_active_idx
    ON refresh_tokens (user_id, device_id)
    WHERE revoked = false;

CREATE INDEX IF NOT EXISTS refresh_tokens_user_idx ON refresh_tokens (user_id);
