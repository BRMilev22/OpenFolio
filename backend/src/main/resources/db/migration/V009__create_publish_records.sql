CREATE TABLE publish_records
(
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    portfolio_id  BIGINT   NOT NULL,
    published_url TEXT     NOT NULL,
    version       INT      NOT NULL DEFAULT 1,
    published_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_publish_records_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
);
