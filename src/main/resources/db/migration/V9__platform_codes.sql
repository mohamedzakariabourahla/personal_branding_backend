-- Add platform codes to decouple display names from publisher matching.
ALTER TABLE platforms ADD COLUMN IF NOT EXISTS code VARCHAR(64);

-- Seed known platform codes.
UPDATE platforms SET code = 'instagram' WHERE name = 'Instagram';
UPDATE platforms SET code = 'tiktok' WHERE name = 'TikTok';
UPDATE platforms SET code = 'youtube' WHERE name = 'YouTube';
UPDATE platforms SET code = 'twitter' WHERE name IN ('Twitter/X', 'Twitter');
UPDATE platforms SET code = 'facebook' WHERE name = 'Facebook';
UPDATE platforms SET code = 'threads' WHERE name = 'Threads';

-- Fallback: if any new rows exist without a code, default to lower(name) with slashes replaced.
UPDATE platforms
SET code = lower(replace(name, '/', '_'))
WHERE code IS NULL;

-- Enforce uniqueness and presence.
ALTER TABLE platforms ALTER COLUMN code SET NOT NULL;
ALTER TABLE platforms ADD CONSTRAINT uq_platform_code UNIQUE (code);
