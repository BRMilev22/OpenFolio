-- Add rich content field to sections (stores About text / README / custom content)
ALTER TABLE sections ADD COLUMN content TEXT;

-- Track which GitHub username was used for ingestion (for re-sync)
ALTER TABLE users ADD COLUMN github_username VARCHAR(255);

-- Fix education table to match entity (add start/end year if missing)
ALTER TABLE education MODIFY COLUMN institution VARCHAR(255) NOT NULL;
