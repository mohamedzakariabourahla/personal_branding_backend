CREATE TABLE IF NOT EXISTS niches (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS audiences (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS tones (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS platforms (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS countries (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    iso_code VARCHAR(3) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS posting_frequencies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    onboarding_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE IF NOT EXISTS persons (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255),
    phone_number VARCHAR(32),
    company_name VARCHAR(255),
    position VARCHAR(255),
    brand_color VARCHAR(32),
    font_style VARCHAR(128)
);

CREATE TABLE IF NOT EXISTS person_niches (
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    niche_id BIGINT NOT NULL REFERENCES niches(id),
    PRIMARY KEY (person_id, niche_id)
);

CREATE TABLE IF NOT EXISTS person_audiences (
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    audience_id BIGINT NOT NULL REFERENCES audiences(id),
    PRIMARY KEY (person_id, audience_id)
);

CREATE TABLE IF NOT EXISTS person_tones (
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    tone_id BIGINT NOT NULL REFERENCES tones(id),
    PRIMARY KEY (person_id, tone_id)
);

CREATE TABLE IF NOT EXISTS person_platforms (
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    platform_id BIGINT NOT NULL REFERENCES platforms(id),
    PRIMARY KEY (person_id, platform_id)
);

CREATE TABLE IF NOT EXISTS person_countries (
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    country_id BIGINT NOT NULL REFERENCES countries(id),
    PRIMARY KEY (person_id, country_id)
);

CREATE TABLE IF NOT EXISTS person_posting_frequencies (
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    posting_frequency_id BIGINT NOT NULL REFERENCES posting_frequencies(id),
    PRIMARY KEY (person_id, posting_frequency_id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
