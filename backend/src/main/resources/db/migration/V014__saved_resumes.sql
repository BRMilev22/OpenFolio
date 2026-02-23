-- ============================================================
-- V014: Saved resume PDFs — persist generated PDFs so users
-- can access, share, and download them without regenerating.
-- ============================================================

CREATE TABLE IF NOT EXISTS saved_resumes (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    portfolio_id    BIGINT          NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    template_key    VARCHAR(50)     NOT NULL DEFAULT 'pdf',
    file_size_bytes BIGINT          NOT NULL DEFAULT 0,
    pdf_data        LONGBLOB        NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_saved_resume_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_resume_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolios(id) ON DELETE CASCADE,

    INDEX idx_saved_resume_user (user_id),
    INDEX idx_saved_resume_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
