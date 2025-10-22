CREATE TYPE hackathon.iban_status AS ENUM ('ALLOW', 'REVIEW', 'BLOCK');

CREATE TABLE hackathon.iban (
    id BIGSERIAL PRIMARY KEY,
    iban VARCHAR(34) UNIQUE NOT NULL,
    status hackathon.iban_status NOT NULL DEFAULT 'REVIEW',
    created_at TIMESTAMP DEFAULT NOW()
);