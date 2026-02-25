-- V016: Make education.degree nullable (users may not always have a degree title)
ALTER TABLE education MODIFY COLUMN degree VARCHAR(255) NULL;
