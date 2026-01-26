-- V17__link_existing_patients_to_users.sql
-- Link existing patient records to user accounts by matching email addresses

-- Update patients table to link to users where email matches
UPDATE patients p
SET user_id = u.id
FROM users u
WHERE p.email = u.email
  AND p.user_id IS NULL
  AND u.id IS NOT NULL;

-- For patients without email, try to match by name pattern
-- This handles cases where patient was created from DICOM with name like "Patient Bob Wilson"
UPDATE patients p
SET email = u.email,
    user_id = u.id
FROM users u
WHERE p.name = u.full_name
  AND p.user_id IS NULL
  AND p.email IS NULL
  AND u.email IS NOT NULL;