-- V1__init_schema.sql
-- Core schema for Intelligent Medical Imaging Diagnosis and PACS System

-- =============================================
-- USERS AND ROLES
-- =============================================

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE,
                       description VARCHAR(255),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       email VARCHAR(255) UNIQUE,
                       phone VARCHAR(50),
                       full_name VARCHAR(200),
                       role_id BIGINT NOT NULL REFERENCES roles(id),
                       enabled BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- PATIENT AND STUDY HIERARCHY
-- =============================================

CREATE TABLE patients (
                          id BIGSERIAL PRIMARY KEY,
                          patient_id VARCHAR(100) NOT NULL UNIQUE,  -- DICOM PatientID
                          name VARCHAR(200),                         -- De-identified or anonymized
                          sex VARCHAR(10),
                          birth_date DATE,
                          meta JSONB,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE studies (
                         id BIGSERIAL PRIMARY KEY,
                         study_uid VARCHAR(128) NOT NULL UNIQUE,   -- DICOM StudyInstanceUID
                         patient_id BIGINT NOT NULL REFERENCES patients(id),
                         study_date TIMESTAMP,
                         description VARCHAR(500),
                         modality VARCHAR(20),                      -- CT, MR, US, XR, etc.
                         accession_number VARCHAR(100),
                         status VARCHAR(50) DEFAULT 'uploaded',    -- uploaded, archived, processed
                         meta JSONB,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE series (
                        id BIGSERIAL PRIMARY KEY,
                        series_uid VARCHAR(128) NOT NULL UNIQUE,  -- DICOM SeriesInstanceUID
                        study_id BIGINT NOT NULL REFERENCES studies(id) ON DELETE CASCADE,
                        series_number INTEGER,
                        modality VARCHAR(20),
                        description VARCHAR(500),
                        manufacturer VARCHAR(200),
                        image_count INTEGER DEFAULT 0,
                        meta JSONB,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE instances (
                           id BIGSERIAL PRIMARY KEY,
                           instance_uid VARCHAR(128) NOT NULL UNIQUE, -- DICOM SOPInstanceUID
                           series_id BIGINT NOT NULL REFERENCES series(id) ON DELETE CASCADE,
                           instance_number INTEGER,
                           file_path VARCHAR(1000) NOT NULL,
                           file_size BIGINT,
                           rows INTEGER,
                           columns INTEGER,
                           pixel_spacing VARCHAR(100),
                           slice_thickness DECIMAL(10, 4),
                           slice_location DECIMAL(10, 4),
                           image_orientation VARCHAR(200),
                           image_position VARCHAR(200),
                           transfer_syntax_uid VARCHAR(128),
                           meta JSONB,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- AI INFERENCE RESULTS
-- =============================================

CREATE TABLE ai_tasks (
                          id BIGSERIAL PRIMARY KEY,
                          task_id VARCHAR(100) NOT NULL UNIQUE,
                          study_id BIGINT NOT NULL REFERENCES studies(id),
                          model_name VARCHAR(100) NOT NULL,
                          model_version VARCHAR(50),
                          status VARCHAR(50) DEFAULT 'pending',     -- pending, running, finished, failed
                          params JSONB,
                          error_message TEXT,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          started_at TIMESTAMP,
                          finished_at TIMESTAMP
);

CREATE TABLE ai_results (
                            id BIGSERIAL PRIMARY KEY,
                            task_id BIGINT NOT NULL REFERENCES ai_tasks(id) ON DELETE CASCADE,
                            result_type VARCHAR(50) NOT NULL,         -- segmentation, classification, detection
                            result_files JSONB,                        -- Paths to mask, heatmap, etc.
                            metrics JSONB,                             -- Dice, confidence, etc.
                            labels JSONB,                              -- Detection labels, bounding boxes
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- DIAGNOSTIC REPORTS
-- =============================================

CREATE TABLE reports (
                         id BIGSERIAL PRIMARY KEY,
                         report_uid VARCHAR(100) NOT NULL UNIQUE,
                         study_id BIGINT NOT NULL REFERENCES studies(id),
                         author_id BIGINT NOT NULL REFERENCES users(id),
                         ai_task_id BIGINT REFERENCES ai_tasks(id),
                         title VARCHAR(500),
                         content JSONB,                             -- Structured report content
                         summary TEXT,
                         findings TEXT,
                         impression TEXT,
                         recommendations TEXT,
                         attachments JSONB,                         -- Diagnostic images, markup paths
                         status VARCHAR(50) DEFAULT 'draft',       -- draft, finalized, amended
                         finalized BOOLEAN DEFAULT FALSE,
                         finalized_at TIMESTAMP,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- AUDIT LOGGING
-- =============================================

CREATE TABLE audit_log (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT REFERENCES users(id),
                           username VARCHAR(100),
                           action VARCHAR(100) NOT NULL,
                           resource_type VARCHAR(100),
                           resource_id VARCHAR(100),
                           ip_address VARCHAR(50),
                           user_agent VARCHAR(500),
                           details JSONB,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- PACS OPERATIONS LOG
-- =============================================

CREATE TABLE pacs_operations (
                                 id BIGSERIAL PRIMARY KEY,
                                 operation_type VARCHAR(50) NOT NULL,      -- PULL, PUSH, C_FIND, C_MOVE, C_STORE
                                 study_uid VARCHAR(128),
                                 series_uid VARCHAR(128),
                                 status VARCHAR(50) DEFAULT 'pending',
                                 request_params JSONB,
                                 response_data JSONB,
                                 error_message TEXT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 completed_at TIMESTAMP
);