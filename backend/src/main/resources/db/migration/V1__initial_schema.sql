CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role IN ('CITIZEN', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE issue_groups (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(24) NOT NULL UNIQUE,
    issue_type VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    severity VARCHAR(16),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address VARCHAR(500),
    area VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(24) NOT NULL UNIQUE,
    citizen_id BIGINT NOT NULL REFERENCES app_users(id),
    issue_group_id BIGINT NOT NULL REFERENCES issue_groups(id),
    issue_type VARCHAR(32) NOT NULL,
    description VARCHAR(1000),
    image_url VARCHAR(500) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address VARCHAR(500),
    area VARCHAR(160),
    severity VARCHAR(16),
    verification_state VARCHAR(24) NOT NULL,
    verification_note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE status_history (
    id BIGSERIAL PRIMARY KEY,
    issue_group_id BIGINT NOT NULL REFERENCES issue_groups(id),
    changed_by BIGINT NOT NULL REFERENCES app_users(id),
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX reports_group_idx ON reports(issue_group_id);
CREATE INDEX reports_citizen_idx ON reports(citizen_id, created_at DESC);
CREATE INDEX issue_groups_location_idx ON issue_groups(latitude, longitude, issue_type);
CREATE INDEX issue_groups_status_idx ON issue_groups(status);
