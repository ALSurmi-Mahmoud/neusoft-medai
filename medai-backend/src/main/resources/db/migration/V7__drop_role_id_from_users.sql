-- Remove the old role_id column since we now use user_roles join table
ALTER TABLE users DROP COLUMN IF EXISTS role_id;