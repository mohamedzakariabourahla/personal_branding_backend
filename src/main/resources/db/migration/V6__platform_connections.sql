CREATE TABLE platform_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform_id BIGINT NOT NULL REFERENCES platforms(id),
    external_account_id VARCHAR(128) NOT NULL,
    external_username VARCHAR(255),
    external_display_name VARCHAR(255),
    account_metadata JSONB,
    status VARCHAR(32) NOT NULL DEFAULT 'CONNECTED',
    last_error TEXT,
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT platform_connections_status_chk CHECK (status IN ('CONNECTED', 'DISCONNECTED', 'ERROR')),
    UNIQUE (user_id, platform_id, external_account_id)
);

CREATE INDEX idx_platform_connections_user ON platform_connections(user_id);
CREATE INDEX idx_platform_connections_platform ON platform_connections(platform_id);

CREATE TABLE platform_tokens (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL REFERENCES platform_connections(id) ON DELETE CASCADE,
    access_token_cipher TEXT,
    access_token_iv BYTEA,
    refresh_token_cipher TEXT,
    refresh_token_iv BYTEA,
    token_type VARCHAR(64),
    scopes TEXT[],
    access_token_expires_at TIMESTAMPTZ,
    refresh_token_expires_at TIMESTAMPTZ,
    last_rotated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fingerprint CHAR(64)
);

CREATE UNIQUE INDEX platform_tokens_connection_uidx ON platform_tokens(connection_id);

CREATE TABLE platform_oauth_states (
    state VARCHAR(128) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform_id BIGINT NOT NULL REFERENCES platforms(id),
    code_verifier VARCHAR(255),
    redirect_uri TEXT,
    requested_scopes TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_platform_oauth_states_expires_at ON platform_oauth_states(expires_at);
