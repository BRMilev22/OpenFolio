CREATE TABLE projects
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    portfolio_id   BIGINT       NOT NULL,
    github_repo_id VARCHAR(255),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    url            TEXT,
    languages      JSON,
    stars          INT          NOT NULL DEFAULT 0,
    forks          INT          NOT NULL DEFAULT 0,
    is_highlighted BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order  INT          NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_projects_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
);
