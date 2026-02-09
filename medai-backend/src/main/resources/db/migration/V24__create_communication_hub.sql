-- ============================================================================
-- Migration V24: Communication Hub - Messaging & Notifications
-- Description: Real-time chat, notifications, email integration
-- Author: MedAI Development Team
-- Date: 2026-02-09
-- ============================================================================

-- Conversations Table (1-to-1 chat between doctor and patient)
CREATE TABLE conversations (
                               id BIGSERIAL PRIMARY KEY,
                               conversation_uid VARCHAR(50) UNIQUE NOT NULL,

    -- Participants
                               doctor_id BIGINT NOT NULL,
                               patient_id BIGINT NOT NULL,

    -- Status
                               status VARCHAR(20) DEFAULT 'active', -- active, archived, closed
                               is_muted BOOLEAN DEFAULT false,

    -- Last message info (for quick access)
                               last_message_at TIMESTAMP,
                               last_message_preview TEXT,

    -- Metadata
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
                               CONSTRAINT fk_conversation_doctor FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT fk_conversation_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
                               CONSTRAINT chk_conversation_status CHECK (status IN ('active', 'archived', 'closed')),
                               CONSTRAINT unique_conversation UNIQUE (doctor_id, patient_id)
);

-- Messages Table
CREATE TABLE messages (
                          id BIGSERIAL PRIMARY KEY,
                          conversation_id BIGINT NOT NULL,

    -- Sender (can be doctor or admin, tracked separately)
                          sender_user_id BIGINT, -- if sent by doctor/admin
                          sender_type VARCHAR(20) NOT NULL, -- doctor, patient, system

    -- Message content
                          message_type VARCHAR(20) DEFAULT 'text', -- text, image, file, system
                          content TEXT,

    -- File attachments
                          file_name VARCHAR(255),
                          file_path VARCHAR(500),
                          file_type VARCHAR(50),
                          file_size BIGINT,

    -- Status tracking
                          is_read BOOLEAN DEFAULT false,
                          read_at TIMESTAMP,
                          is_deleted BOOLEAN DEFAULT false,

    -- Metadata
                          sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
                          CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                          CONSTRAINT fk_message_sender FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE SET NULL,
                          CONSTRAINT chk_sender_type CHECK (sender_type IN ('doctor', 'patient', 'system')),
                          CONSTRAINT chk_message_type CHECK (message_type IN ('text', 'image', 'file', 'system'))
);

-- Message Templates Table
CREATE TABLE message_templates (
                                   id BIGSERIAL PRIMARY KEY,
                                   name VARCHAR(200) NOT NULL,
                                   category VARCHAR(100), -- appointment_reminder, lab_results, prescription, general

                                   subject VARCHAR(300),
                                   content TEXT NOT NULL,

    -- Template variables (JSONB) - e.g., {patient_name}, {doctor_name}, {date}
                                   variables JSONB,

    -- Metadata
                                   is_system BOOLEAN DEFAULT false,
                                   created_by BIGINT,
                                   usage_count INTEGER DEFAULT 0,

                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                   CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Notifications Table (in-app + email + push)
CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,

    -- Recipient
                               user_id BIGINT NOT NULL,

    -- Notification content
                               type VARCHAR(50) NOT NULL, -- message, appointment, prescription, report, system
                               title VARCHAR(300) NOT NULL,
                               content TEXT,

    -- Related entity (polymorphic)
                               related_entity_type VARCHAR(50), -- message, appointment, prescription, etc.
                               related_entity_id BIGINT,

    -- Action link
                               action_url VARCHAR(500),

    -- Status
                               is_read BOOLEAN DEFAULT false,
                               read_at TIMESTAMP,

    -- Delivery channels
                               sent_in_app BOOLEAN DEFAULT true,
                               sent_email BOOLEAN DEFAULT false,
                               sent_push BOOLEAN DEFAULT false,

                               email_sent_at TIMESTAMP,
                               push_sent_at TIMESTAMP,

    -- Priority
                               priority VARCHAR(20) DEFAULT 'normal', -- low, normal, high, urgent

    -- Expiry (auto-delete old notifications)
                               expires_at TIMESTAMP,

                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                               CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT chk_notification_type CHECK (type IN ('message', 'appointment', 'prescription', 'report', 'treatment_plan', 'system')),
                               CONSTRAINT chk_notification_priority CHECK (priority IN ('low', 'normal', 'high', 'urgent'))
);

-- Notification Preferences Table
CREATE TABLE notification_preferences (
                                          id BIGSERIAL PRIMARY KEY,
                                          user_id BIGINT UNIQUE NOT NULL,

    -- Channel preferences
                                          enable_in_app BOOLEAN DEFAULT true,
                                          enable_email BOOLEAN DEFAULT true,
                                          enable_push BOOLEAN DEFAULT false,

    -- Type preferences (JSONB)
    -- e.g., {"messages": {"in_app": true, "email": true}, "appointments": {...}}
                                          type_preferences JSONB,

    -- Quiet hours
                                          quiet_hours_enabled BOOLEAN DEFAULT false,
                                          quiet_hours_start TIME,
                                          quiet_hours_end TIME,

                                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                          CONSTRAINT fk_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Email Queue Table (for async email sending)
CREATE TABLE email_queue (
                             id BIGSERIAL PRIMARY KEY,

    -- Recipient
                             to_email VARCHAR(200) NOT NULL,
                             to_name VARCHAR(200),

    -- Email content
                             subject VARCHAR(500) NOT NULL,
                             body TEXT NOT NULL,

    -- Status
                             status VARCHAR(20) DEFAULT 'pending', -- pending, sent, failed
                             attempts INTEGER DEFAULT 0,
                             max_attempts INTEGER DEFAULT 3,

                             error_message TEXT,

    -- Timestamps
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                             sent_at TIMESTAMP,

                             CONSTRAINT chk_email_status CHECK (status IN ('pending', 'sent', 'failed'))
);

-- Indexes for Performance
CREATE INDEX idx_conversations_doctor ON conversations(doctor_id);
CREATE INDEX idx_conversations_patient ON conversations(patient_id);
CREATE INDEX idx_conversations_last_message ON conversations(last_message_at DESC);
CREATE INDEX idx_conversations_status ON conversations(status);

CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_sent_at ON messages(sent_at DESC);
CREATE INDEX idx_messages_sender ON messages(sender_user_id);
CREATE INDEX idx_messages_unread ON messages(conversation_id, is_read) WHERE is_read = false;
CREATE INDEX idx_messages_not_deleted ON messages(is_deleted) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_templates_category ON message_templates(category);
CREATE INDEX IF NOT EXISTS idx_templates_is_system ON message_templates(is_system);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = false;
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_expires ON notifications(expires_at) WHERE expires_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_email_queue_status ON email_queue(status);
CREATE INDEX IF NOT EXISTS idx_email_queue_created ON email_queue(created_at);

-- Function to Generate Conversation UID
CREATE OR REPLACE FUNCTION generate_conversation_uid()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.conversation_uid IS NULL OR NEW.conversation_uid = '' THEN
        NEW.conversation_uid := 'CONV-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' || LPAD(NEXTVAL('conversations_id_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_generate_conversation_uid
    BEFORE INSERT ON conversations
    FOR EACH ROW
EXECUTE FUNCTION generate_conversation_uid();

-- Function to Update Conversation on New Message
CREATE OR REPLACE FUNCTION update_conversation_on_message()
    RETURNS TRIGGER AS $$
BEGIN
    UPDATE conversations
    SET last_message_at = NEW.sent_at,
        last_message_preview = SUBSTRING(NEW.content, 1, 100),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.conversation_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_conversation_on_message
    AFTER INSERT ON messages
    FOR EACH ROW
EXECUTE FUNCTION update_conversation_on_message();

-- Function to Update Timestamps
CREATE OR REPLACE FUNCTION update_communication_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_conversation_timestamp
    BEFORE UPDATE ON conversations
    FOR EACH ROW
EXECUTE FUNCTION update_communication_timestamp();

CREATE TRIGGER trigger_update_template_timestamp
    BEFORE UPDATE ON message_templates
    FOR EACH ROW
EXECUTE FUNCTION update_communication_timestamp();

CREATE TRIGGER trigger_update_preferences_timestamp
    BEFORE UPDATE ON notification_preferences
    FOR EACH ROW
EXECUTE FUNCTION update_communication_timestamp();

-- Insert System Message Templates
INSERT INTO message_templates (name, category, subject, content, is_system, variables) VALUES
                                                                                           (
                                                                                               'Appointment Reminder',
                                                                                               'appointment_reminder',
                                                                                               'Reminder: Upcoming Appointment',
                                                                                               'Dear {patient_name},

                                                                                           This is a reminder that you have an appointment scheduled with Dr. {doctor_name} on {appointment_date} at {appointment_time}.

                                                                                           Location: {location}

                                                                                           Please arrive 15 minutes early. If you need to reschedule, please contact us at your earliest convenience.

                                                                                           Best regards,
                                                                                           {clinic_name}',
                                                                                               true,
                                                                                               '{"patient_name": "string", "doctor_name": "string", "appointment_date": "date", "appointment_time": "time", "location": "string", "clinic_name": "string"}'::jsonb
                                                                                           ),
                                                                                           (
                                                                                               'Lab Results Available',
                                                                                               'lab_results',
                                                                                               'Your Lab Results are Ready',
                                                                                               'Dear {patient_name},

                                                                                           Your lab results from {test_date} are now available for review. Please log in to your patient portal to view them, or contact your doctor''s office.

                                                                                           Dr. {doctor_name} will discuss the results with you during your next appointment or may contact you if any immediate action is needed.

                                                                                           Thank you,
                                                                                           {clinic_name}',
                                                                                               true,
                                                                                               '{"patient_name": "string", "doctor_name": "string", "test_date": "date", "clinic_name": "string"}'::jsonb
                                                                                           ),
                                                                                           (
                                                                                               'Prescription Ready',
                                                                                               'prescription',
                                                                                               'Your Prescription is Ready',
                                                                                               'Dear {patient_name},

                                                                                           Your prescription from Dr. {doctor_name} is ready for pickup.

                                                                                           Medication: {medication_name}
                                                                                           Instructions: {instructions}

                                                                                           You can pick it up at {pharmacy_name} during business hours.

                                                                                           Best regards,
                                                                                           {clinic_name}',
                                                                                               true,
                                                                                               '{"patient_name": "string", "doctor_name": "string", "medication_name": "string", "instructions": "string", "pharmacy_name": "string", "clinic_name": "string"}'::jsonb
                                                                                           ),
                                                                                           (
                                                                                               'Treatment Plan Update',
                                                                                               'general',
                                                                                               'Your Treatment Plan has been Updated',
                                                                                               'Dear {patient_name},

                                                                                           Dr. {doctor_name} has updated your treatment plan. Please log in to your patient portal to review the changes.

                                                                                           If you have any questions, please don''t hesitate to contact us.

                                                                                           Best regards,
                                                                                           {clinic_name}',
                                                                                               true,
                                                                                               '{"patient_name": "string", "doctor_name": "string", "clinic_name": "string"}'::jsonb
                                                                                           ),
                                                                                           (
                                                                                               'Welcome Message',
                                                                                               'general',
                                                                                               'Welcome to Our Patient Portal',
                                                                                               'Dear {patient_name},

                                                                                           Welcome to our patient portal! We''re glad to have you as a patient.

                                                                                           You can now:
                                                                                           - Message your doctor directly
                                                                                           - View your medical records
                                                                                           - Schedule appointments
                                                                                           - Receive test results

                                                                                           If you need any assistance, please don''t hesitate to reach out.

                                                                                           Best regards,
                                                                                           {clinic_name}',
                                                                                               true,
                                                                                               '{"patient_name": "string", "clinic_name": "string"}'::jsonb
                                                                                           );

-- Comments
COMMENT ON TABLE conversations IS 'Chat conversations between doctors and patients';
COMMENT ON TABLE messages IS 'Individual messages within conversations';
COMMENT ON TABLE message_templates IS 'Reusable message templates';
COMMENT ON TABLE notifications IS 'User notifications (in-app, email, push)';
COMMENT ON TABLE notification_preferences IS 'User notification preferences';
COMMENT ON TABLE email_queue IS 'Async email sending queue';

COMMENT ON COLUMN conversations.conversation_uid IS 'Unique identifier format: CONV-YYYYMMDD-XXXXXX';
COMMENT ON COLUMN messages.sender_type IS 'doctor, patient, or system';
COMMENT ON COLUMN notifications.type IS 'message, appointment, prescription, report, treatment_plan, system';
COMMENT ON COLUMN notifications.priority IS 'low, normal, high, urgent';

-- Success Message
DO $$
    BEGIN
        RAISE NOTICE '✅ Migration V24 completed successfully!';
        RAISE NOTICE '💬 Communication Hub created';
        RAISE NOTICE '📧 5 system templates added';
        RAISE NOTICE '🔔 Notification system ready';
        RAISE NOTICE '⚡ Real-time messaging enabled';
    END $$;