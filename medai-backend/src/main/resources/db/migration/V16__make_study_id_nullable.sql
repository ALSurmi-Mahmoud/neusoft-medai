-- Make study_id nullable in reports table to support general reports
-- (reports not tied to imaging studies)

ALTER TABLE reports
    ALTER COLUMN study_id DROP NOT NULL;

-- Add a comment explaining this change
COMMENT ON COLUMN reports.study_id IS 'Optional: Links to Study for imaging reports. NULL for general clinical reports.';