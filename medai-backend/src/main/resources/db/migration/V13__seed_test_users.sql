-- Seed test users for each role
-- Password: password123 (BCrypt hash)
-- Hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

-- Insert test users
INSERT INTO users (username, password_hash, email, full_name, enabled) VALUES
                                                                           ('doctor1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'doctor1@medai.local', 'Dr. John Smith', true),
                                                                           ('nurse1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'nurse1@medai.local', 'Nurse Jane Doe', true),
                                                                           ('patient1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'patient1@medai.local', 'Patient Bob Wilson', true),
                                                                           ('tech1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'tech1@medai.local', 'Tech Alice Brown', true),
                                                                           ('researcher1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'researcher1@medai.local', 'Researcher Chris Lee', true)
ON CONFLICT (username) DO NOTHING;

-- Assign roles to test users
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'doctor1' AND r.name = 'DOCTOR'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'nurse1' AND r.name = 'NURSE'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'patient1' AND r.name = 'PATIENT'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'tech1' AND r.name = 'TECHNICIAN'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'researcher1' AND r.name = 'RESEARCHER'
ON CONFLICT (user_id, role_id) DO NOTHING;
