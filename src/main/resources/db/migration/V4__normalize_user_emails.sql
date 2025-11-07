-- Normalize existing emails to lower-case and trim whitespace
UPDATE users
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL;

-- Drop legacy unique constraint generated on raw column casing
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

-- Ensure case-insensitive uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS users_email_lower_idx ON users ((LOWER(email)));
