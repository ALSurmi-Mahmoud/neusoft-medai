-- First ensure ADMIN role exists
INSERT INTO roles (name)
VALUES ('ADMIN')
    ON CONFLICT (name) DO NOTHING;

-- Get the admin user id and admin role id, then insert into user_roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN'
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- Also ensure other roles exist
INSERT INTO roles (name) VALUES ('DOCTOR') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('NURSE') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('PATIENT') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('RESEARCHER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('TECHNICIAN') ON CONFLICT (name) DO NOTHING;