CREATE TABLE portfolios
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    slug         VARCHAR(255) NOT NULL,
    title        VARCHAR(255),
    tagline      TEXT,
    is_published BOOLEAN      NOT NULL DEFAULT FALSE,
    theme_key    VARCHAR(100) NOT NULL DEFAULT 'light',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolios_slug (slug),
    CONSTRAINT fk_portfolios_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
