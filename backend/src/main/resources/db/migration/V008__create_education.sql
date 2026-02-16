CREATE TABLE education
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    portfolio_id  BIGINT       NOT NULL,
    institution   VARCHAR(255) NOT NULL,
    degree        VARCHAR(255),
    field         VARCHAR(255),
    start_year    INT,
    end_year      INT,
    display_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_education_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
);
