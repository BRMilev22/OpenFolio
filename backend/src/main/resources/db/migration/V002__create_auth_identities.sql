CREATE TABLE auth_identities
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    provider      ENUM ('GITHUB', 'LINKEDIN', 'EMAIL') NOT NULL,
    provider_uid  VARCHAR(255) NOT NULL,
    access_token  TEXT,
    refresh_token TEXT,
    token_expires DATETIME,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_identity (provider, provider_uid),
    CONSTRAINT fk_auth_identities_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
