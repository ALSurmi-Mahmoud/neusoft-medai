-- ============================================================================
-- Migration V23: Treatment Plans System
-- Description: Multi-step treatment tracking with progress monitoring
-- Author: MedAI Development Team
-- Date: 2026-01-29
-- ============================================================================

-- Treatment Plans Table
CREATE TABLE treatment_plans (
                                 id BIGSERIAL PRIMARY KEY,
                                 plan_uid VARCHAR(50) UNIQUE NOT NULL,

    -- Relationships
                                 patient_id BIGINT NOT NULL,
                                 doctor_id BIGINT NOT NULL,

    -- Plan Details
                                 title VARCHAR(200) NOT NULL,
                                 diagnosis VARCHAR(500),
                                 goals TEXT,
                                 description TEXT,

    -- Dates
                                 start_date DATE NOT NULL,
                                 end_date DATE,
                                 expected_completion_date DATE,
                                 actual_completion_date DATE,

    -- Status & Progress
                                 status VARCHAR(50) DEFAULT 'active', -- active, completed, cancelled, on_hold
                                 progress_percentage INTEGER DEFAULT 0,

    -- Priority & Category
                                 priority VARCHAR(20) DEFAULT 'medium', -- low, medium, high, urgent
                                 category VARCHAR(100), -- chronic_disease, post_operative, rehabilitation, preventive

    -- Metadata
                                 notes TEXT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
                                 CONSTRAINT fk_plan_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_plan_doctor FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE SET NULL,
                                 CONSTRAINT chk_status CHECK (status IN ('active', 'completed', 'cancelled', 'on_hold')),
                                 CONSTRAINT chk_priority CHECK (priority IN ('low', 'medium', 'high', 'urgent')),
                                 CONSTRAINT chk_progress CHECK (progress_percentage >= 0 AND progress_percentage <= 100)
);

-- Treatment Steps Table
CREATE TABLE treatment_steps (
                                 id BIGSERIAL PRIMARY KEY,
                                 plan_id BIGINT NOT NULL,

    -- Step Details
                                 step_order INTEGER NOT NULL,
                                 title VARCHAR(200) NOT NULL,
                                 description TEXT,
                                 instructions TEXT,

    -- Duration
                                 duration_days INTEGER,
                                 due_date DATE,

    -- Status
                                 status VARCHAR(50) DEFAULT 'pending', -- pending, in_progress, completed, skipped, delayed
                                 completed_at TIMESTAMP,

    -- Assigned Resources
                                 assigned_to BIGINT, -- doctor or nurse user_id

    -- Tracking
                                 notes TEXT,
                                 completion_notes TEXT,

    -- Metadata
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
                                 CONSTRAINT fk_step_plan FOREIGN KEY (plan_id) REFERENCES treatment_plans(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_step_assigned FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
                                 CONSTRAINT chk_step_status CHECK (status IN ('pending', 'in_progress', 'completed', 'skipped', 'delayed'))
);

-- Treatment Plan Templates Table
CREATE TABLE treatment_plan_templates (
                                          id BIGSERIAL PRIMARY KEY,
                                          name VARCHAR(200) NOT NULL,
                                          category VARCHAR(100),
                                          description TEXT,

    -- Template Content (JSONB)
                                          template_data JSONB NOT NULL,

    -- Metadata
                                          is_system BOOLEAN DEFAULT false,
                                          created_by BIGINT,
                                          specialty VARCHAR(100),

    -- Usage Stats
                                          usage_count INTEGER DEFAULT 0,

                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                          CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Plan Milestones Table (for tracking major achievements)
CREATE TABLE plan_milestones (
                                 id BIGSERIAL PRIMARY KEY,
                                 plan_id BIGINT NOT NULL,

                                 title VARCHAR(200) NOT NULL,
                                 description TEXT,
                                 target_date DATE,
                                 achieved_date TIMESTAMP,
                                 is_achieved BOOLEAN DEFAULT false,

                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                 CONSTRAINT fk_milestone_plan FOREIGN KEY (plan_id) REFERENCES treatment_plans(id) ON DELETE CASCADE
);

-- Plan Attachments Table (link prescriptions, notes, reports)
CREATE TABLE plan_attachments (
                                  id BIGSERIAL PRIMARY KEY,
                                  plan_id BIGINT NOT NULL,

    -- Attachment Type
                                  attachment_type VARCHAR(50) NOT NULL, -- prescription, clinical_note, report, document
                                  attachment_id BIGINT, -- ID of the linked entity

    -- File Attachment (optional)
                                  file_name VARCHAR(255),
                                  file_path VARCHAR(500),
                                  file_type VARCHAR(50),

                                  notes TEXT,
                                  attached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                  CONSTRAINT fk_attachment_plan FOREIGN KEY (plan_id) REFERENCES treatment_plans(id) ON DELETE CASCADE,
                                  CONSTRAINT chk_attachment_type CHECK (attachment_type IN ('prescription', 'clinical_note', 'report', 'document', 'file'))
);

-- Indexes for Performance
CREATE INDEX idx_plans_patient_id ON treatment_plans(patient_id);
CREATE INDEX idx_plans_doctor_id ON treatment_plans(doctor_id);
CREATE INDEX idx_plans_status ON treatment_plans(status);
CREATE INDEX idx_plans_start_date ON treatment_plans(start_date);
CREATE INDEX idx_plans_priority ON treatment_plans(priority);
CREATE INDEX idx_plans_category ON treatment_plans(category);

CREATE INDEX idx_steps_plan_id ON treatment_steps(plan_id);
CREATE INDEX idx_steps_status ON treatment_steps(status);
CREATE INDEX idx_steps_due_date ON treatment_steps(due_date);
CREATE INDEX idx_steps_assigned_to ON treatment_steps(assigned_to);
CREATE INDEX idx_steps_order ON treatment_steps(plan_id, step_order);

CREATE INDEX idx_templates_category ON treatment_plan_templates(category);
-- Drop the index if it already exists
DROP INDEX IF EXISTS idx_templates_is_system;

-- Create the index
CREATE INDEX idx_templates_is_system ON treatment_plan_templates(is_system);

CREATE INDEX idx_templates_usage ON treatment_plan_templates(usage_count DESC);

CREATE INDEX idx_milestones_plan_id ON plan_milestones(plan_id);
CREATE INDEX idx_milestones_achieved ON plan_milestones(is_achieved);

CREATE INDEX idx_attachments_plan_id ON plan_attachments(plan_id);
CREATE INDEX idx_attachments_type ON plan_attachments(attachment_type);

-- Function to Generate Plan UID
CREATE OR REPLACE FUNCTION generate_plan_uid()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.plan_uid IS NULL OR NEW.plan_uid = '' THEN
        NEW.plan_uid := 'PLAN-' || TO_CHAR(NEW.start_date, 'YYYYMMDD') || '-' || LPAD(NEXTVAL('treatment_plans_id_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_generate_plan_uid
    BEFORE INSERT ON treatment_plans
    FOR EACH ROW
EXECUTE FUNCTION generate_plan_uid();

-- Function to Update Plan Progress
CREATE OR REPLACE FUNCTION update_plan_progress()
    RETURNS TRIGGER AS $$
DECLARE
    total_steps INTEGER;
    completed_steps INTEGER;
    new_progress INTEGER;
BEGIN
    -- Count total steps
    SELECT COUNT(*) INTO total_steps
    FROM treatment_steps
    WHERE plan_id = NEW.plan_id AND status != 'skipped';

    -- Count completed steps
    SELECT COUNT(*) INTO completed_steps
    FROM treatment_steps
    WHERE plan_id = NEW.plan_id AND status = 'completed';

    -- Calculate progress
    IF total_steps > 0 THEN
        new_progress := (completed_steps * 100) / total_steps;
    ELSE
        new_progress := 0;
    END IF;

    -- Update plan progress
    UPDATE treatment_plans
    SET progress_percentage = new_progress,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.plan_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_plan_progress
    AFTER UPDATE OF status ON treatment_steps
    FOR EACH ROW
    WHEN (OLD.status IS DISTINCT FROM NEW.status)
EXECUTE FUNCTION update_plan_progress();

-- Function to Auto-Complete Plan
CREATE OR REPLACE FUNCTION auto_complete_plan()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.progress_percentage = 100 AND NEW.status = 'active' THEN
        NEW.status := 'completed';
        NEW.actual_completion_date := CURRENT_DATE;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_auto_complete_plan
    BEFORE UPDATE OF progress_percentage ON treatment_plans
    FOR EACH ROW
EXECUTE FUNCTION auto_complete_plan();

-- Function to Update Timestamps
CREATE OR REPLACE FUNCTION update_treatment_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_plan_timestamp
    BEFORE UPDATE ON treatment_plans
    FOR EACH ROW
EXECUTE FUNCTION update_treatment_timestamp();

CREATE TRIGGER trigger_update_step_timestamp
    BEFORE UPDATE ON treatment_steps
    FOR EACH ROW
EXECUTE FUNCTION update_treatment_timestamp();

-- Insert System Templates

-- Template 1: Post-Operative Care
INSERT INTO treatment_plan_templates (name, category, is_system, specialty, description, template_data)
VALUES (
           'Post-Operative Care Plan',
           'post_operative',
           true,
           'general',
           'Standard post-operative recovery plan',
           '{
             "title": "Post-Operative Care",
             "duration_days": 14,
             "steps": [
               {
                 "order": 1,
                 "title": "Immediate Post-Op Monitoring",
                 "description": "Monitor vital signs and wound healing",
                 "duration_days": 2,
                 "instructions": "Check vitals every 4 hours, monitor for infection signs"
               },
               {
                 "order": 2,
                 "title": "Pain Management",
                 "description": "Manage post-operative pain",
                 "duration_days": 7,
                 "instructions": "Prescribed pain medication, ice packs as needed"
               },
               {
                 "order": 3,
                 "title": "Wound Care",
                 "description": "Proper wound care and dressing changes",
                 "duration_days": 10,
                 "instructions": "Keep wound clean and dry, change dressing daily"
               },
               {
                 "order": 4,
                 "title": "Follow-up Appointment",
                 "description": "Post-op check with surgeon",
                 "duration_days": 14,
                 "instructions": "Schedule follow-up to remove stitches/staples"
               }
             ]
           }'::jsonb
       );

-- Template 2: Diabetes Management
INSERT INTO treatment_plan_templates (name, category, is_system, specialty, description, template_data)
VALUES (
           'Diabetes Management Plan',
           'chronic_disease',
           true,
           'endocrinology',
           'Comprehensive diabetes management',
           '{
             "title": "Diabetes Management",
             "duration_days": 90,
             "steps": [
               {
                 "order": 1,
                 "title": "Initial Assessment",
                 "description": "Baseline labs and vital signs",
                 "duration_days": 7,
                 "instructions": "HbA1c, fasting glucose, lipid panel"
               },
               {
                 "order": 2,
                 "title": "Medication Initiation",
                 "description": "Start diabetes medications",
                 "duration_days": 14,
                 "instructions": "Metformin 500mg BID, titrate as needed"
               },
               {
                 "order": 3,
                 "title": "Diet & Exercise Plan",
                 "description": "Lifestyle modification counseling",
                 "duration_days": 30,
                 "instructions": "Low glycemic diet, 30 min exercise 5x/week"
               },
               {
                 "order": 4,
                 "title": "Blood Glucose Monitoring",
                 "description": "Daily glucose tracking",
                 "duration_days": 90,
                 "instructions": "Check fasting and 2-hour post-meal glucose"
               },
               {
                 "order": 5,
                 "title": "Follow-up Labs",
                 "description": "Monitor treatment effectiveness",
                 "duration_days": 90,
                 "instructions": "Repeat HbA1c, adjust medications as needed"
               }
             ]
           }'::jsonb
       );

-- Template 3: Hypertension Management
INSERT INTO treatment_plan_templates (name, category, is_system, specialty, description, template_data)
VALUES (
           'Hypertension Management Plan',
           'chronic_disease',
           true,
           'cardiology',
           'Blood pressure control program',
           '{
             "title": "Hypertension Management",
             "duration_days": 60,
             "steps": [
               {
                 "order": 1,
                 "title": "Baseline Assessment",
                 "description": "Initial BP monitoring and labs",
                 "duration_days": 7,
                 "instructions": "24-hour BP monitoring, renal function tests"
               },
               {
                 "order": 2,
                 "title": "Medication Start",
                 "description": "Initiate antihypertensive therapy",
                 "duration_days": 14,
                 "instructions": "Lisinopril 10mg daily, monitor for side effects"
               },
               {
                 "order": 3,
                 "title": "Lifestyle Modifications",
                 "description": "DASH diet and exercise",
                 "duration_days": 30,
                 "instructions": "Low sodium diet, regular aerobic exercise"
               },
               {
                 "order": 4,
                 "title": "Home BP Monitoring",
                 "description": "Daily blood pressure tracking",
                 "duration_days": 60,
                 "instructions": "Check BP twice daily, keep log"
               },
               {
                 "order": 5,
                 "title": "Medication Adjustment",
                 "description": "Titrate medications to target BP",
                 "duration_days": 60,
                 "instructions": "Adjust dose based on BP readings"
               }
             ]
           }'::jsonb
       );

-- Template 4: Physical Therapy Recovery
INSERT INTO treatment_plan_templates (name, category, is_system, specialty, description, template_data)
VALUES (
           'Physical Therapy Recovery',
           'rehabilitation',
           true,
           'orthopedics',
           'Post-injury or post-surgery PT plan',
           '{
             "title": "Physical Therapy Program",
             "duration_days": 42,
             "steps": [
               {
                 "order": 1,
                 "title": "Initial PT Evaluation",
                 "description": "Assess range of motion and strength",
                 "duration_days": 7,
                 "instructions": "Baseline measurements, set recovery goals"
               },
               {
                 "order": 2,
                 "title": "Passive Range of Motion",
                 "description": "Gentle mobility exercises",
                 "duration_days": 14,
                 "instructions": "PT sessions 3x/week, home exercises daily"
               },
               {
                 "order": 3,
                 "title": "Strength Building",
                 "description": "Progressive resistance training",
                 "duration_days": 21,
                 "instructions": "Increase weight gradually, focus on form"
               },
               {
                 "order": 4,
                 "title": "Functional Training",
                 "description": "Return to normal activities",
                 "duration_days": 35,
                 "instructions": "Activity-specific exercises, balance training"
               },
               {
                 "order": 5,
                 "title": "Final Assessment",
                 "description": "Evaluate recovery completion",
                 "duration_days": 42,
                 "instructions": "Compare to baseline, clear for full activity"
               }
             ]
           }'::jsonb
       );

-- Template 5: Preventive Health Screening
INSERT INTO treatment_plan_templates (name, category, is_system, specialty, description, template_data)
VALUES (
           'Annual Preventive Health Screening',
           'preventive',
           true,
           'general',
           'Comprehensive annual health checkup',
           '{
             "title": "Annual Health Screening",
             "duration_days": 30,
             "steps": [
               {
                 "order": 1,
                 "title": "Physical Examination",
                 "description": "Complete physical exam",
                 "duration_days": 7,
                 "instructions": "Vital signs, general health assessment"
               },
               {
                 "order": 2,
                 "title": "Laboratory Tests",
                 "description": "Comprehensive metabolic panel",
                 "duration_days": 14,
                 "instructions": "CBC, lipid panel, glucose, kidney/liver function"
               },
               {
                 "order": 3,
                 "title": "Cancer Screenings",
                 "description": "Age-appropriate cancer screening",
                 "duration_days": 21,
                 "instructions": "Mammogram, colonoscopy, PSA as indicated"
               },
               {
                 "order": 4,
                 "title": "Vaccinations",
                 "description": "Update immunizations",
                 "duration_days": 28,
                 "instructions": "Flu shot, Tdap, other age-appropriate vaccines"
               },
               {
                 "order": 5,
                 "title": "Results Review",
                 "description": "Discuss findings and recommendations",
                 "duration_days": 30,
                 "instructions": "Review all results, create action plan if needed"
               }
             ]
           }'::jsonb
       );

-- Comments
COMMENT ON TABLE treatment_plans IS 'Multi-step treatment plans for patient care';
COMMENT ON TABLE treatment_steps IS 'Individual steps within a treatment plan';
COMMENT ON TABLE treatment_plan_templates IS 'Reusable treatment plan templates';
COMMENT ON TABLE plan_milestones IS 'Major milestones within treatment plans';
COMMENT ON TABLE plan_attachments IS 'Linked prescriptions, notes, reports, files';

COMMENT ON COLUMN treatment_plans.plan_uid IS 'Unique identifier format: PLAN-YYYYMMDD-XXXXXX';
COMMENT ON COLUMN treatment_plans.status IS 'active, completed, cancelled, on_hold';
COMMENT ON COLUMN treatment_plans.progress_percentage IS 'Auto-calculated from completed steps';
COMMENT ON COLUMN treatment_steps.status IS 'pending, in_progress, completed, skipped, delayed';

-- Success Message
DO $$
    BEGIN
        RAISE NOTICE '✅ Migration V23 completed successfully!';
        RAISE NOTICE '📋 Treatment Plans system created';
        RAISE NOTICE '🎯 5 system templates added';
        RAISE NOTICE '⚡ Auto-progress calculation enabled';
    END $$;