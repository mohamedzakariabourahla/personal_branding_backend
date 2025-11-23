CREATE TABLE publishing_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    platform_id BIGINT NOT NULL,
    connection_id BIGINT NOT NULL,
    media_asset_ids TEXT,
    caption TEXT,
    scheduled_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_tried_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    failure_reason TEXT,
    external_post_id VARCHAR(255)
);

CREATE INDEX idx_publishing_jobs_scheduled_status ON publishing_jobs (scheduled_at) WHERE status = 'SCHEDULED';
