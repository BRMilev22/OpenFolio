CREATE TABLE experiences
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    portfolio_id  BIGINT       NOT NULL,
    company       VARCHAR(255) NOT NULL,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    start_date    DATE,
    end_date      DATE,
    is_current    BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_experiences_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
);
