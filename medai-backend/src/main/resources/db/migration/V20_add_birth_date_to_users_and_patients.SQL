-- V20__add_birth_date_to_users_and_patients.sql
-- Add birth_date column to users table and ensure patients have required fields

-- ============================================================================
-- ADD BIRTH_DATE TO USERS TABLE
-- ============================================================================

-- Add birth_date column to users (all roles need age/birth date)
ALTER TABLE users ADD COLUMN IF NOT EXISTS birth_date DATE;

-- Add comment
COMMENT ON COLUMN users.birth_date IS 'Date of birth for all users (doctors, nurses, patients, etc.)';

-- ============================================================================
-- ENSURE PATIENTS TABLE HAS PROPER CONSTRAINTS
-- ============================================================================

-- Make sure sex and birth_date are captured for patients
-- Note: We won't make them NOT NULL yet to avoid breaking existing data
-- But we'll add check constraints

-- Add address fields for patients (optional but useful)
ALTER TABLE patients ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS state VARCHAR(50);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS zip_code VARCHAR(20);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS country VARCHAR(100);

-- Add emergency contact
ALTER TABLE patients ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(200);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(20);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS emergency_contact_relationship VARCHAR(100);

-- Add medical fields
ALTER TABLE patients ADD COLUMN IF NOT EXISTS blood_type VARCHAR(10);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS allergies TEXT;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS medical_conditions TEXT;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS current_medications TEXT;
ALTER TABLE patients ADD COLUMN IF NOT EXISTS notes TEXT;

-- Add insurance info (optional)
ALTER TABLE patients ADD COLUMN IF NOT EXISTS insurance_provider VARCHAR(200);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS insurance_policy_number VARCHAR(100);

-- Add phone if not exists
ALTER TABLE patients ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

-- ============================================================================
-- CREATE INDEX FOR PERFORMANCE
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_users_birth_date ON users(birth_date);
CREATE INDEX IF NOT EXISTS idx_patients_birth_date ON patients(birth_date);
CREATE INDEX IF NOT EXISTS idx_patients_phone ON patients(phone);

-- ============================================================================
-- BACKFILL DATA (Optional - for existing users)
-- ============================================================================

-- For existing patients without birth_date, set a default far in past
-- This is just to indicate "unknown" - admin should update these
UPDATE patients
SET birth_date = '1900-01-01'
WHERE birth_date IS NULL;

-- Set default sex to 'U' (Unknown) for existing patients without sex
UPDATE patients
SET sex = 'U'
WHERE sex IS NULL;

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON COLUMN patients.address IS 'Street address of patient';
COMMENT ON COLUMN patients.emergency_contact_name IS 'Emergency contact full name';
COMMENT ON COLUMN patients.blood_type IS 'Blood type: A+, A-, B+, B-, O+, O-, AB+, AB-';
COMMENT ON COLUMN patients.allergies IS 'Known allergies (medications, food, environmental)';
COMMENT ON COLUMN patients.medical_conditions IS 'Chronic conditions, diagnoses';
COMMENT ON COLUMN patients.current_medications IS 'Current medications and dosages';

-- ============================================================================
-- END OF V20 MIGRATION
-- ============================================================================