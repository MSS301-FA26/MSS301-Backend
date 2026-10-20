ALTER TABLE users
    ADD COLUMN username VARCHAR(255),
    ADD COLUMN identity_number VARCHAR(12),
    ADD COLUMN preferred_cinema_id BIGINT;

ALTER TABLE users
    ADD CONSTRAINT uk_users_username UNIQUE (username),
    ADD CONSTRAINT uk_users_identity_number UNIQUE (identity_number);

ALTER TABLE pending_registrations
    ADD COLUMN username VARCHAR(255),
    ADD COLUMN identity_number VARCHAR(12),
    ADD COLUMN preferred_cinema_id BIGINT;

CREATE INDEX idx_pending_registrations_username ON pending_registrations(username);
CREATE INDEX idx_pending_registrations_identity_number ON pending_registrations(identity_number);
