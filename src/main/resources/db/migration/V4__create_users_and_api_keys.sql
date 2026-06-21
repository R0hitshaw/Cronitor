CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'USER',
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_username ON users (username);

COMMENT ON COLUMN users.role IS 'USER | ADMIN';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password — never store plain text';

-- ----------------------------------------------------------

CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key_hash        VARCHAR(255)    NOT NULL UNIQUE,  -- SHA-256 hash of the actual key
    name            VARCHAR(100)    NOT NULL,          -- human label e.g. "Production server"
    last_used_at    TIMESTAMPTZ,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_api_keys_user_id  ON api_keys (user_id);
CREATE INDEX idx_api_keys_key_hash ON api_keys (key_hash);

COMMENT ON COLUMN api_keys.key_hash IS 'SHA-256 of the raw key. Raw key shown only once on creation.';
