-- V18__add_patient_id_to_reports.sql
-- Add optional patient_id to reports for reports created without studies

ALTER TABLE reports
    ADD COLUMN patient_id BIGINT;

-- Add foreign key constraint
ALTER TABLE reports
    ADD CONSTRAINT fk_reports_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id)
            ON DELETE SET NULL;

-- Update existing reports to link patient_id from study->patient
UPDATE reports r
SET patient_id = s.patient_id
FROM studies s
WHERE r.study_id = s.id
  AND r.patient_id IS NULL;

-- Create index for performance
CREATE INDEX idx_reports_patient_id ON reports(patient_id);