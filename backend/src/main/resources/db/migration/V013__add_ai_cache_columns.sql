-- ============================================================
-- V013: Cache AI-enhanced descriptions in the database
-- so they persist across server restarts and don't need
-- to be regenerated each time.
-- ============================================================

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'projects' AND COLUMN_NAME = 'ai_enhanced_description');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE projects ADD COLUMN ai_enhanced_description TEXT NULL, ADD COLUMN ai_enhanced_at DATETIME NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists2 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portfolios' AND COLUMN_NAME = 'ai_enhanced_summary');
SET @sql2 = IF(@col_exists2 = 0,
  'ALTER TABLE portfolios ADD COLUMN ai_enhanced_summary TEXT NULL, ADD COLUMN ai_enhanced_at DATETIME NULL',
  'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
