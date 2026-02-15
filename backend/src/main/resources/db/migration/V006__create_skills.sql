CREATE TABLE skills
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    portfolio_id BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(100),
    proficiency  ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'),
    display_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_skills_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
);
