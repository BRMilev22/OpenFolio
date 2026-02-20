CREATE TABLE resume_exports
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    portfolio_id  BIGINT       NOT NULL,
    file_url      TEXT         NOT NULL,
    template_key  VARCHAR(100) NOT NULL DEFAULT 'default',
    generated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_resume_exports_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
);
