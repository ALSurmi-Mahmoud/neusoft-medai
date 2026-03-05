-- ============================================================================
-- Migration V28: System Administration
-- Description: System settings, user management, and audit logs
-- ============================================================================

-- System Settings Table
CREATE TABLE IF NOT EXISTS system_settings (
                                               id BIGSERIAL PRIMARY KEY,

    -- Application Info
                                               app_name VARCHAR(200) DEFAULT 'MedAI PACS',
                                               app_version VARCHAR(50) DEFAULT '1.0.0',

    -- Email Settings
                                               smtp_enabled BOOLEAN DEFAULT false,
                                               smtp_host VARCHAR(200),
                                               smtp_port INTEGER DEFAULT 587,
                                               smtp_username VARCHAR(200),
                                               smtp_password VARCHAR(500), -- Encrypted
                                               smtp_from_email VARCHAR(200),
                                               smtp_use_tls BOOLEAN DEFAULT true,

    -- Security Settings
                                               session_timeout_minutes INTEGER DEFAULT 60,
                                               password_min_length INTEGER DEFAULT 8,
                                               max_login_attempts INTEGER DEFAULT 5,
                                               lockout_duration_minutes INTEGER DEFAULT 30,

    -- Storage Settings
                                               max_upload_size_mb INTEGER DEFAULT 100,
                                               storage_quota_gb INTEGER DEFAULT 500,

    -- Maintenance
                                               maintenance_mode BOOLEAN DEFAULT false,
                                               maintenance_message TEXT,

    -- Timestamps
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                               updated_by BIGINT,

                                               CONSTRAINT fk_settings_user FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Login Attempts Tracking
CREATE TABLE IF NOT EXISTS login_attempts (
                                              id BIGSERIAL PRIMARY KEY,
                                              username VARCHAR(100) NOT NULL,
                                              ip_address VARCHAR(50),
                                              success BOOLEAN NOT NULL,
                                              failure_reason VARCHAR(200),
                                              attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_login_attempts_username ON login_attempts(username);
CREATE INDEX idx_login_attempts_time ON login_attempts(attempted_at DESC);

-- System Activity Log
CREATE TABLE IF NOT EXISTS system_activity_log (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   activity_type VARCHAR(100) NOT NULL,
                                                   description TEXT NOT NULL,
                                                   performed_by BIGINT,
                                                   ip_address VARCHAR(50),
                                                   status VARCHAR(50) NOT NULL,
                                                   metadata JSONB,
                                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                                   CONSTRAINT fk_activity_user FOREIGN KEY (performed_by) REFERENCES users(id)
);

CREATE INDEX idx_activity_type ON system_activity_log(activity_type);
CREATE INDEX idx_activity_time ON system_activity_log(created_at DESC);
CREATE INDEX idx_activity_user ON system_activity_log(performed_by);

-- Enhance audit_logs indexes
CREATE INDEX IF NOT EXISTS idx_audit_created_desc ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_user_action ON audit_logs(user_id, action);

-- Insert default settings
INSERT INTO system_settings (app_name, app_version)
VALUES ('MedAI PACS', '1.0.0')
ON CONFLICT DO NOTHING;

-- Success message
DO $$
    BEGIN
        RAISE NOTICE '✅ Migration V28 completed - System Administration ready!';
    END $$;