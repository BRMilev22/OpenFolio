-- ============================================================
-- V012: Resume builder – dedicated resume entity
-- A "resume" is a curated snapshot backed by portfolio data
-- but with its own template choice, ordering, and user overrides.
-- ============================================================

CREATE TABLE resumes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    portfolio_id    BIGINT        NOT NULL,
    title           VARCHAR(200)  NOT NULL DEFAULT 'My Resume',
    template_key    VARCHAR(30)   NOT NULL DEFAULT 'classic',
    -- user-editable header overrides (nullable = fall back to user/portfolio)
    full_name       VARCHAR(200),
    job_title       VARCHAR(200),
    email           VARCHAR(200),
    phone           VARCHAR(50),
    location        VARCHAR(200),
    website         VARCHAR(500),
    linkedin_url    VARCHAR(500),
    github_url      VARCHAR(500),
    summary         TEXT,
    -- JSON arrays of IDs to control which items appear & order
    selected_project_ids    JSON,
    selected_skill_ids      JSON,
    selected_experience_ids JSON,
    selected_education_ids  JSON,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
