-- Add publish columns to saved_resumes (idempotent)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name   = 'saved_resumes'
                     AND column_name  = 'publish_token');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE saved_resumes ADD COLUMN publish_token VARCHAR(64) UNIQUE, ADD COLUMN published_at DATETIME',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
