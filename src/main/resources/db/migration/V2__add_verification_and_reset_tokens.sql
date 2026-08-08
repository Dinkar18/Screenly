ALTER TABLE users ADD COLUMN is_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE verification_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_verification FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_password_reset FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
