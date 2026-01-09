-- V2__seed_roles_admin.sql
-- Seed default roles and admin user

-- Insert default roles
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN', 'System Administrator - full access to all features'),
                                          ('DOCTOR', 'Clinician/Radiologist - view images, create reports'),
                                          ('RESEARCHER', 'Researcher/Student - model training, data export'),
                                          ('TECHNICIAN', 'Nurse/Technician - upload images, basic data entry');

-- Insert admin user
-- Password: admin123 (BCrypt hash)
-- To regenerate: Use https://bcrypt-generator.com/ or Spring Security BCryptPasswordEncoder
INSERT INTO users (username, password_hash, email, full_name, role_id, enabled)
VALUES (
           'admin',
           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.PwTLspQFO5oTR0HJRm',
           'admin@medai.local',
           'System Administrator',
           (SELECT id FROM roles WHERE name = 'ADMIN'),
           TRUE
       );