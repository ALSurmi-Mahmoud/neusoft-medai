-- Analytics performance indexes
CREATE INDEX IF NOT EXISTS idx_patients_created ON patients(created_at);
CREATE INDEX IF NOT EXISTS idx_patients_birth_date ON patients(birth_date);

CREATE INDEX IF NOT EXISTS idx_studies_created ON studies(created_at);
CREATE INDEX IF NOT EXISTS idx_studies_date ON studies(study_date);
CREATE INDEX IF NOT EXISTS idx_studies_modality ON studies(modality);
CREATE INDEX IF NOT EXISTS idx_studies_status ON studies(status);

CREATE INDEX IF NOT EXISTS idx_reports_created ON reports(created_at);
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_author ON reports(author_id);

CREATE INDEX IF NOT EXISTS idx_treatments_created ON treatment_plans(created_at);
CREATE INDEX IF NOT EXISTS idx_treatments_status ON treatment_plans(status);
CREATE INDEX IF NOT EXISTS idx_treatments_category ON treatment_plans(category);

CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);