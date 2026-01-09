-- V3__add_indexes.sql
-- Performance indexes for common query patterns

-- User lookups
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);

-- Patient lookups
CREATE INDEX idx_patients_patient_id ON patients(patient_id);

-- Study lookups and filtering
CREATE INDEX idx_studies_study_uid ON studies(study_uid);
CREATE INDEX idx_studies_patient_id ON studies(patient_id);
CREATE INDEX idx_studies_study_date ON studies(study_date);
CREATE INDEX idx_studies_modality ON studies(modality);
CREATE INDEX idx_studies_status ON studies(status);

-- Series lookups
CREATE INDEX idx_series_series_uid ON series(series_uid);
CREATE INDEX idx_series_study_id ON series(study_id);

-- Instance lookups
CREATE INDEX idx_instances_instance_uid ON instances(instance_uid);
CREATE INDEX idx_instances_series_id ON instances(series_id);

-- AI task lookups
CREATE INDEX idx_ai_tasks_task_id ON ai_tasks(task_id);
CREATE INDEX idx_ai_tasks_study_id ON ai_tasks(study_id);
CREATE INDEX idx_ai_tasks_status ON ai_tasks(status);

-- Report lookups
CREATE INDEX idx_reports_report_uid ON reports(report_uid);
CREATE INDEX idx_reports_study_id ON reports(study_id);
CREATE INDEX idx_reports_author_id ON reports(author_id);
CREATE INDEX idx_reports_status ON reports(status);

-- Audit log queries
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_log_resource ON audit_log(resource_type, resource_id);

-- PACS operations
CREATE INDEX idx_pacs_operations_study_uid ON pacs_operations(study_uid);
CREATE INDEX idx_pacs_operations_status ON pacs_operations(status);