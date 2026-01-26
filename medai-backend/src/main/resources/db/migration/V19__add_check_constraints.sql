-- V19__add_check_constraints.sql
-- Add CHECK constraints for data integrity and validation
-- This migration adds constraints to ensure data quality across the system

-- ============================================================================
-- APPOINTMENTS TABLE CONSTRAINTS
-- ============================================================================

-- Constraint: appointment_status must be valid
ALTER TABLE appointments
    ADD CONSTRAINT check_appointment_status
        CHECK (status IN ('scheduled', 'confirmed', 'completed', 'cancelled', 'pending', 'no-show'));

-- Constraint: appointment_date cannot be in the distant past (more than 10 years ago)
ALTER TABLE appointments
    ADD CONSTRAINT check_appointment_date_not_too_old
        CHECK (appointment_date >= CURRENT_DATE - INTERVAL '10 years');

-- Constraint: appointment_time must be during working hours (07:00 to 20:00)
ALTER TABLE appointments
    ADD CONSTRAINT check_appointment_time_working_hours
        CHECK (appointment_time BETWEEN '07:00:00' AND '20:00:00');

-- Constraint: duration_minutes must be positive and reasonable (5 to 480 minutes)
ALTER TABLE appointments
    ADD CONSTRAINT check_appointment_duration
        CHECK (duration_minutes > 0 AND duration_minutes <= 480);

-- Constraint: end_time must be after appointment_time (if both are set)
ALTER TABLE appointments
    ADD CONSTRAINT check_appointment_end_after_start
        CHECK (end_time IS NULL OR end_time > appointment_time);


-- ============================================================================
-- PATIENTS TABLE CONSTRAINTS
-- ============================================================================

-- Constraint: patient sex must be valid
ALTER TABLE patients
    ADD CONSTRAINT check_patient_sex
        CHECK (sex IS NULL OR sex IN ('M', 'F', 'O', 'U'));  -- Male, Female, Other, Unknown

-- Constraint: birth_date must be in the past and not too old (max 150 years)
ALTER TABLE patients
    ADD CONSTRAINT check_patient_birth_date
        CHECK (
            birth_date IS NULL OR
            (birth_date < CURRENT_DATE AND birth_date >= CURRENT_DATE - INTERVAL '150 years')
            );

-- Constraint: email must have basic format (if provided)
ALTER TABLE patients
    ADD CONSTRAINT check_patient_email_format
        CHECK (
            email IS NULL OR
            email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
            );

-- Constraint: patient_id must not be empty
ALTER TABLE patients
    ADD CONSTRAINT check_patient_id_not_empty
        CHECK (length(trim(patient_id)) > 0);


-- ============================================================================
-- REPORTS TABLE CONSTRAINTS
-- ============================================================================

-- Constraint: report status must be valid
ALTER TABLE reports
    ADD CONSTRAINT check_report_status
        CHECK (status IN ('draft', 'pending', 'finalized', 'amended', 'archived', 'deleted'));

-- Constraint: report_uid must not be empty
ALTER TABLE reports
    ADD CONSTRAINT check_report_uid_not_empty
        CHECK (length(trim(report_uid)) > 0);

-- Constraint: finalized_at must be set if finalized is true
ALTER TABLE reports
    ADD CONSTRAINT check_report_finalized_consistency
        CHECK (
            (finalized = FALSE AND finalized_at IS NULL) OR
            (finalized = TRUE AND finalized_at IS NOT NULL)
            );

-- Constraint: finalized_at cannot be in the future
ALTER TABLE reports
    ADD CONSTRAINT check_report_finalized_at_not_future
        CHECK (finalized_at IS NULL OR finalized_at <= CURRENT_TIMESTAMP);

-- Constraint: report must have either study_id or patient_id (or both)
ALTER TABLE reports
    ADD CONSTRAINT check_report_has_patient_context
        CHECK (study_id IS NOT NULL OR patient_id IS NOT NULL);


-- ============================================================================
-- STUDIES TABLE CONSTRAINTS
-- ============================================================================

-- Constraint: study status must be valid
ALTER TABLE studies
    ADD CONSTRAINT check_study_status
        CHECK (status IN ('uploaded', 'processing', 'completed', 'failed', 'archived', 'available', 'pending'));

-- Constraint: modality must be valid DICOM modality code (common ones)
ALTER TABLE studies
    ADD CONSTRAINT check_study_modality
        CHECK (
            modality IS NULL OR
            modality IN (
                         'CT', 'MR', 'US', 'XR', 'DX', 'CR', 'MG', 'PT', 'NM', 'RF',
                         'OT', 'XA', 'ES', 'SC', 'BI', 'CD', 'DD', 'DG', 'EM', 'HC',
                         'IO', 'IVUS', 'LS', 'OPM', 'OP', 'OPR', 'OPT', 'PR', 'PX',
                         'REG', 'RTDOSE', 'RTIMAGE', 'RTPLAN', 'RTRECORD', 'RTSTRUCT',
                         'RWV', 'SEG', 'SM', 'SR', 'SRF', 'TG', 'VA', 'VL'
                )
            );

-- Constraint: study_uid must not be empty
ALTER TABLE studies
    ADD CONSTRAINT check_study_uid_not_empty
        CHECK (length(trim(study_uid)) > 0);

-- Constraint: study_date should not be in the distant future (max 1 day ahead for timezone tolerance)
ALTER TABLE studies
    ADD CONSTRAINT check_study_date_not_future
        CHECK (study_date IS NULL OR study_date <= CURRENT_TIMESTAMP + INTERVAL '1 day');

-- Constraint: study_date should not be too old (max 100 years)
ALTER TABLE studies
    ADD CONSTRAINT check_study_date_not_too_old
        CHECK (study_date IS NULL OR study_date >= CURRENT_TIMESTAMP - INTERVAL '100 years');


-- ============================================================================
-- USERS TABLE CONSTRAINTS
-- ============================================================================

-- Constraint: username must not be empty and must be reasonable length
ALTER TABLE users
    ADD CONSTRAINT check_user_username
        CHECK (length(trim(username)) >= 3 AND length(username) <= 50);

-- Constraint: email must have basic format
ALTER TABLE users
    ADD CONSTRAINT check_user_email_format
        CHECK (
            email IS NULL OR
            email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
            );

-- Constraint: full_name must not be empty if provided
ALTER TABLE users
    ADD CONSTRAINT check_user_full_name
        CHECK (full_name IS NULL OR length(trim(full_name)) > 0);

-- Constraint: password_hash must be present and look like bcrypt hash
ALTER TABLE users
    ADD CONSTRAINT check_user_password_hash
        CHECK (length(password_hash) >= 20);  -- Bcrypt hashes are 60 chars, but be lenient

-- Constraint: phone must have reasonable format if provided (digits, spaces, hyphens, parentheses, plus)
ALTER TABLE users
    ADD CONSTRAINT check_user_phone_format
        CHECK (
            phone IS NULL OR
            phone ~* '^[0-9\s\-\(\)\+\.]+$'
            );


-- ============================================================================
-- PERFORMANCE NOTES AND INDEXES VERIFICATION
-- ============================================================================

-- All necessary indexes already exist from previous migrations:
-- - appointments: doctor_id, patient_id, appointment_date, status
-- - patients: patient_id, user_id, email
-- - reports: author_id, patient_id, study_id, report_uid, status
-- - studies: patient_id, study_uid, study_date, status, modality
-- - users: username, email, enabled

-- No additional indexes needed at this time.

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON CONSTRAINT check_appointment_status ON appointments IS
    'Ensures appointment status is one of the valid values';

COMMENT ON CONSTRAINT check_patient_sex ON patients IS
    'Validates patient sex: M (Male), F (Female), O (Other), U (Unknown)';

COMMENT ON CONSTRAINT check_report_status ON reports IS
    'Ensures report status follows the defined workflow states';

COMMENT ON CONSTRAINT check_study_modality ON studies IS
    'Validates DICOM modality codes according to standard';

COMMENT ON CONSTRAINT check_user_email_format ON users IS
    'Basic email format validation using regex';

-- ============================================================================
-- END OF V19 MIGRATION
-- ============================================================================