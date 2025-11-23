CREATE TABLE publishing_attempts (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES publishing_jobs(id) ON DELETE CASCADE,
    attempted_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,
    error TEXT,
    provider_response TEXT
);

CREATE INDEX idx_publishing_attempts_job_id ON publishing_attempts(job_id);
