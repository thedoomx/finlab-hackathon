CREATE TABLE IF NOT EXISTS hackathon.auth_sessions (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    username VARCHAR(255) NOT NULL,
    login_time TIMESTAMP NOT NULL,
    logout_time TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_auth_sessions_username ON hackathon.auth_sessions(username);
CREATE INDEX idx_auth_sessions_status ON hackathon.auth_sessions(status);
CREATE INDEX idx_auth_sessions_login_time ON hackathon.auth_sessions(login_time);
