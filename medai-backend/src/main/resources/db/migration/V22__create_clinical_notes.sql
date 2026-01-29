-- ============================================================================
-- Migration V22: Clinical Notes & Documentation System
-- Description: Complete SOAP notes and clinical documentation
-- Author: MedAI Development Team
-- Date: 2024
-- ============================================================================

-- Clinical Notes Table
CREATE TABLE clinical_notes (
    id BIGSERIAL PRIMARY KEY,
    note_uid VARCHAR(50) UNIQUE NOT NULL,

    -- Relationships
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_id BIGINT,
    study_id BIGINT,

    -- Note Details
    note_type VARCHAR(50) NOT NULL, -- soap, progress, consultation, procedure, discharge
    title VARCHAR(200),

    -- SOAP Components (for SOAP notes)
    subjective TEXT,
    objective TEXT,
    assessment TEXT,
    plan TEXT,

    -- General Content (for non-SOAP notes)
    content TEXT,

    -- Vital Signs (optional, for SOAP notes)
    vitals JSONB, -- {"bp": "120/80", "hr": "72", "temp": "98.6", "rr": "16", "spo2": "98"}

    -- Status
    status VARCHAR(50) DEFAULT 'draft', -- draft, finalized
    finalized BOOLEAN DEFAULT false,
    finalized_at TIMESTAMP,

    -- Timestamps
    note_date DATE DEFAULT CURRENT_DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_note_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_note_doctor FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_note_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    CONSTRAINT fk_note_study FOREIGN KEY (study_id) REFERENCES studies(id) ON DELETE SET NULL,

    CONSTRAINT chk_note_type CHECK (note_type IN ('soap', 'progress', 'consultation', 'procedure', 'discharge', 'followup')),
    CONSTRAINT chk_status CHECK (status IN ('draft', 'finalized'))
);

-- Note Templates Table (for common conditions)
CREATE TABLE note_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    note_type VARCHAR(50) NOT NULL,
    description TEXT,

    -- Template Content
    subjective_template TEXT,
    objective_template TEXT,
    assessment_template TEXT,
    plan_template TEXT,
    content_template TEXT,

    -- Template Metadata
    is_system BOOLEAN DEFAULT false,
    created_by BIGINT,
    specialty VARCHAR(100), -- cardiology, orthopedics, general, etc.

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Note Attachments (link external documents to notes)
CREATE TABLE note_attachments (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL,

    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,

    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_attachment_note FOREIGN KEY (note_id) REFERENCES clinical_notes(id) ON DELETE CASCADE
);

-- Indexes for Performance
CREATE INDEX idx_notes_patient_id ON clinical_notes(patient_id);
CREATE INDEX idx_notes_doctor_id ON clinical_notes(doctor_id);
CREATE INDEX idx_notes_appointment_id ON clinical_notes(appointment_id);
CREATE INDEX idx_notes_study_id ON clinical_notes(study_id);
CREATE INDEX idx_notes_note_date ON clinical_notes(note_date);
CREATE INDEX idx_notes_note_type ON clinical_notes(note_type);
CREATE INDEX idx_notes_status ON clinical_notes(status);
CREATE INDEX idx_notes_created_at ON clinical_notes(created_at);

CREATE INDEX idx_templates_note_type ON note_templates(note_type);
CREATE INDEX idx_templates_specialty ON note_templates(specialty);
CREATE INDEX idx_templates_is_system ON note_templates(is_system);

CREATE INDEX idx_attachments_note_id ON note_attachments(note_id);

-- Full-Text Search Index (for searching note content)
CREATE INDEX idx_notes_search ON clinical_notes USING gin(
    to_tsvector('english',
        coalesce(title, '') || ' ' ||
        coalesce(subjective, '') || ' ' ||
        coalesce(objective, '') || ' ' ||
        coalesce(assessment, '') || ' ' ||
        coalesce(plan, '') || ' ' ||
        coalesce(content, '')
    )
);

-- Function to Generate Note UID
CREATE OR REPLACE FUNCTION generate_note_uid()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.note_uid IS NULL OR NEW.note_uid = '' THEN
        NEW.note_uid := 'NOTE-' || TO_CHAR(NEW.note_date, 'YYYYMMDD') || '-' || LPAD(NEXTVAL('clinical_notes_id_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_generate_note_uid
BEFORE INSERT ON clinical_notes
FOR EACH ROW
EXECUTE FUNCTION generate_note_uid();

-- Function to Update updated_at Timestamp
CREATE OR REPLACE FUNCTION update_note_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_note_timestamp
BEFORE UPDATE ON clinical_notes
FOR EACH ROW
EXECUTE FUNCTION update_note_timestamp();

CREATE TRIGGER trigger_update_template_timestamp
BEFORE UPDATE ON note_templates
FOR EACH ROW
EXECUTE FUNCTION update_note_timestamp();

-- Insert System Templates (Common SOAP Templates)

-- Template 1: General SOAP Note
INSERT INTO note_templates (name, note_type, is_system, specialty, description, subjective_template, objective_template, assessment_template, plan_template)
VALUES (
    'General SOAP Note',
    'soap',
    true,
    'general',
    'Standard SOAP note template for general consultations',
    'Chief Complaint:
Symptoms:
Duration:
Associated symptoms:
Aggravating factors:
Relieving factors:
Previous treatments:',
    'Vital Signs:
- Blood Pressure:
- Heart Rate:
- Temperature:
- Respiratory Rate:
- SpO2:

Physical Examination:
- General Appearance:
- HEENT:
- Cardiovascular:
- Respiratory:
- Abdomen:
- Extremities:
- Neurological:',
    'Diagnosis:
Differential Diagnosis:
Clinical Impression:',
    'Treatment Plan:
1. Medications:
2. Laboratory/Imaging Orders:
3. Patient Education:
4. Follow-up:
5. Referrals:'
);

-- Template 2: Progress Note
INSERT INTO note_templates (name, note_type, is_system, specialty, description, content_template)
VALUES (
    'Progress Note',
    'progress',
    true,
    'general',
    'Standard progress note for follow-up visits',
    'Interval History:

Current Status:

Changes Since Last Visit:

Adherence to Treatment Plan:

New Concerns:

Examination Findings:

Assessment:

Plan:'
);

-- Template 3: Consultation Note
INSERT INTO note_templates (name, note_type, is_system, specialty, description, content_template)
VALUES (
    'Consultation Note',
    'consultation',
    true,
    'general',
    'Specialist consultation note template',
    'Reason for Consultation:

Referring Physician:

History of Present Illness:

Past Medical History:

Medications:

Allergies:

Review of Systems:

Physical Examination:

Diagnostic Test Results:

Impression:

Recommendations:'
);

-- Template 4: Procedure Note
INSERT INTO note_templates (name, note_type, is_system, specialty, description, content_template)
VALUES (
    'Procedure Note',
    'procedure',
    true,
    'general',
    'Standard procedure documentation template',
    'Procedure:

Indication:

Patient Consent: Obtained [ ] Not Obtained [ ]

Anesthesia:

Technique:

Findings:

Complications: None [ ] Yes [ ] (describe):

Estimated Blood Loss:

Specimens:

Post-Procedure Condition:

Plan:'
);

-- Template 5: Discharge Summary
INSERT INTO note_templates (name, note_type, is_system, specialty, description, content_template)
VALUES (
    'Discharge Summary',
    'discharge',
    true,
    'general',
    'Hospital discharge summary template',
    'Admission Date:
Discharge Date:

Admitting Diagnosis:
Discharge Diagnosis:

Hospital Course:

Procedures Performed:

Medications on Admission:

Medications on Discharge:

Discharge Instructions:
1. Activity:
2. Diet:
3. Wound Care:
4. Medications:

Follow-up Appointments:

Discharge Condition:

Patient Education:

Pending Results at Discharge:'
);

-- Comments
COMMENT ON TABLE clinical_notes IS 'Stores all types of clinical documentation including SOAP notes';
COMMENT ON TABLE note_templates IS 'Pre-defined templates for common clinical note types';
COMMENT ON TABLE note_attachments IS 'File attachments linked to clinical notes';

COMMENT ON COLUMN clinical_notes.note_uid IS 'Unique identifier format: NOTE-YYYYMMDD-XXXXXX';
COMMENT ON COLUMN clinical_notes.note_type IS 'Type: soap, progress, consultation, procedure, discharge, followup';
COMMENT ON COLUMN clinical_notes.vitals IS 'JSON object containing vital signs measurements';
COMMENT ON COLUMN clinical_notes.status IS 'draft = editable, finalized = locked';

-- Success Message
DO $$
BEGIN
    RAISE NOTICE '✅ Migration V22 completed successfully!';
    RAISE NOTICE '📝 Clinical Notes system created';
    RAISE NOTICE '📋 5 system templates added';
    RAISE NOTICE '🔍 Full-text search enabled';
END $$;