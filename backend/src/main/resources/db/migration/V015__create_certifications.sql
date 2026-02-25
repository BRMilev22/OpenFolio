CREATE TABLE IF NOT EXISTS certifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id    BIGINT       NOT NULL,
    name            VARCHAR(255) NOT NULL,
    issuing_organization VARCHAR(255),
    issue_date      DATE,
    expiry_date     DATE,
    credential_id   VARCHAR(255),
    credential_url  VARCHAR(500),
    display_order   INT DEFAULT 0,
    CONSTRAINT fk_certifications_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
);
