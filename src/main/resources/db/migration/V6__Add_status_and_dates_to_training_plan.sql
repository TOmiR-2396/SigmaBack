-- Add status and date columns to training_plan table
ALTER TABLE planes ADD COLUMN start_date DATE NOT NULL DEFAULT CURDATE();
ALTER TABLE planes ADD COLUMN end_date DATE DEFAULT NULL;
ALTER TABLE planes ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';
