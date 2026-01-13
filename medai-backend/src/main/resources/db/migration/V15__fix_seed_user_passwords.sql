-- V14__fix_seed_user_passwords.sql
-- Update seed user passwords with correctly generated BCrypt hashes
-- Password for all users: password123
-- Newly generated hash: $2a$10$nXtz1I7ycuNIV47FPrxzcuRT1F50cla3FTDS.qKSy.G9IX9W4vZrK

-- Update test users (doctor1, nurse1, patient1, tech1, researcher1)
UPDATE users
SET password_hash = '$2a$10$nXtz1I7ycuNIV47FPrxzcuRT1F50cla3FTDS.qKSy.G9IX9W4vZrK'
WHERE username IN ('doctor1', 'nurse1', 'patient1', 'tech1', 'researcher1');

-- Verify admin user exists and update if needed
-- Admin password is admin123, different from others
-- Admin hash should remain: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.PwTLspQFO5oTR0HJRm
-- This is already correct from V2 migration, no update needed for admin
