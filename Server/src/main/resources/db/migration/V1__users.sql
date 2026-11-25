CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(24) NOT NULL,
    password_hash CHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX users_username_lower_idx ON users (LOWER(username));
