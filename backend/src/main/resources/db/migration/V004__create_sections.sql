CREATE TABLE sections
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    portfolio_id  BIGINT      NOT NULL,
    type          ENUM ('ABOUT', 'SKILLS', 'PROJECTS', 'EXPERIENCE', 'EDUCATION', 'CONTACT', 'CUSTOM') NOT NULL,
    title         VARCHAR(255),
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    display_order INT         NOT NULL DEFAULT 0,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_sections_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
);
