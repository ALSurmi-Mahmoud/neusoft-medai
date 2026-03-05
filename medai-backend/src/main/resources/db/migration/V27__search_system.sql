-- ============================================================================
-- Migration V27: Advanced Search System
-- Description: Add saved searches and full-text search indexes
-- Author: MedAI Development Team
-- Date: 2026-03-04
-- ============================================================================

-- Saved Searches Table
CREATE TABLE IF NOT EXISTS saved_searches (
                                              id BIGSERIAL PRIMARY KEY,

    -- Basic Info
                                              name VARCHAR(200) NOT NULL,
                                              description TEXT,

    -- Search Configuration
                                              entity_type VARCHAR(50) NOT NULL, -- patients, studies, reports, treatments, notes, all
                                              search_query TEXT,

    -- Filter Criteria (JSONB for flexibility)
    -- Structure: {field: "name", operator: "contains", value: "John", ...}
                                              filters JSONB,

    -- Quick Filter Type (if it's a predefined quick filter)
                                              quick_filter_type VARCHAR(100), -- new_patients, pending_reports, active_plans, etc.

    -- Metadata
                                              is_public BOOLEAN DEFAULT false,
                                              is_quick_filter BOOLEAN DEFAULT false,
                                              execution_count INTEGER DEFAULT 0,

    -- User
                                              created_by BIGINT NOT NULL,

    -- Timestamps
                                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                              last_executed_at TIMESTAMP,

    -- Constraints
                                              CONSTRAINT fk_saved_search_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Search History Table (optional, for analytics)
CREATE TABLE IF NOT EXISTS search_history (
                                              id BIGSERIAL PRIMARY KEY,

                                              user_id BIGINT NOT NULL,
                                              search_query TEXT NOT NULL,
                                              entity_type VARCHAR(50),
                                              results_count INTEGER,

                                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                              CONSTRAINT fk_search_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Full-Text Search Indexes
-- Patient search index
CREATE INDEX IF NOT EXISTS idx_patients_name_fulltext ON patients USING gin(to_tsvector('english', name));
CREATE INDEX IF NOT EXISTS idx_patients_patient_id ON patients(patient_id);
CREATE INDEX IF NOT EXISTS idx_patients_phone ON patients(phone);
CREATE INDEX IF NOT EXISTS idx_patients_email ON patients(email);

-- Study search indexes
CREATE INDEX IF NOT EXISTS idx_studies_accession ON studies(accession_number);
CREATE INDEX IF NOT EXISTS idx_studies_description_fulltext ON studies USING gin(to_tsvector('english', COALESCE(description, '')));

-- Report search indexes
CREATE INDEX IF NOT EXISTS idx_reports_title_fulltext ON reports USING gin(to_tsvector('english', COALESCE(title, '')));
CREATE INDEX IF NOT EXISTS idx_reports_findings_fulltext ON reports USING gin(to_tsvector('english', COALESCE(findings, '')));
CREATE INDEX IF NOT EXISTS idx_reports_impression_fulltext ON reports USING gin(to_tsvector('english', COALESCE(impression, '')));

-- Treatment plan search indexes
CREATE INDEX IF NOT EXISTS idx_treatments_title_fulltext ON treatment_plans USING gin(to_tsvector('english', COALESCE(title, '')));
CREATE INDEX IF NOT EXISTS idx_treatments_diagnosis_fulltext ON treatment_plans USING gin(to_tsvector('english', COALESCE(diagnosis, '')));
CREATE INDEX IF NOT EXISTS idx_treatments_goals_fulltext ON treatment_plans USING gin(to_tsvector('english', COALESCE(goals, '')));

-- Clinical notes search indexes
CREATE INDEX IF NOT EXISTS idx_notes_title_fulltext ON clinical_notes USING gin(to_tsvector('english', COALESCE(title, '')));
CREATE INDEX IF NOT EXISTS idx_notes_subjective_fulltext ON clinical_notes USING gin(to_tsvector('english', COALESCE(subjective, '')));
CREATE INDEX IF NOT EXISTS idx_notes_assessment_fulltext ON clinical_notes USING gin(to_tsvector('english', COALESCE(assessment, '')));

-- Saved searches indexes
CREATE INDEX IF NOT EXISTS idx_saved_searches_user ON saved_searches(created_by);
CREATE INDEX IF NOT EXISTS idx_saved_searches_type ON saved_searches(entity_type);
CREATE INDEX IF NOT EXISTS idx_saved_searches_quick ON saved_searches(is_quick_filter);
CREATE INDEX IF NOT EXISTS idx_saved_searches_public ON saved_searches(is_public);

-- Search history indexes
CREATE INDEX IF NOT EXISTS idx_search_history_user ON search_history(user_id);
CREATE INDEX IF NOT EXISTS idx_search_history_created ON search_history(created_at DESC);

-- Update timestamp trigger for saved_searches
CREATE OR REPLACE FUNCTION update_saved_search_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_saved_search_timestamp ON saved_searches;

CREATE TRIGGER trigger_update_saved_search_timestamp
    BEFORE UPDATE ON saved_searches
    FOR EACH ROW
EXECUTE FUNCTION update_saved_search_timestamp();

-- Function to increment execution count
CREATE OR REPLACE FUNCTION increment_search_execution()
    RETURNS TRIGGER AS $$
BEGIN
    UPDATE saved_searches
    SET execution_count = execution_count + 1,
        last_executed_at = CURRENT_TIMESTAMP
    WHERE id = NEW.id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Insert default quick filters
INSERT INTO saved_searches (name, description, entity_type, quick_filter_type, is_quick_filter, is_public, created_by)
SELECT * FROM (VALUES
                   ('New Patients', 'Patients registered in the last 7 days', 'patients', 'new_patients', true, true, 1),
                   ('Pending Reports', 'Reports in draft status', 'reports', 'pending_reports', true, true, 1),
                   ('Active Treatment Plans', 'Currently active treatment plans', 'treatments', 'active_plans', true, true, 1),
                   ('Today''s Studies', 'Studies uploaded today', 'studies', 'todays_studies', true, true, 1),
                   ('Critical Reports', 'Reports with critical findings', 'reports', 'critical_reports', true, true, 1)
              ) AS t(name, description, entity_type, quick_filter_type, is_quick_filter, is_public, created_by)
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1)
ON CONFLICT DO NOTHING;

-- Comments
COMMENT ON TABLE saved_searches IS 'User-defined and system-defined saved searches';
COMMENT ON TABLE search_history IS 'Search query history for analytics';
COMMENT ON COLUMN saved_searches.filters IS 'JSONB object containing filter criteria';
COMMENT ON COLUMN saved_searches.quick_filter_type IS 'Predefined quick filter type identifier';

-- Success message
DO $$
    BEGIN
        RAISE NOTICE '✅ Migration V27 completed successfully!';
        RAISE NOTICE '🔍 Search system tables created';
        RAISE NOTICE '📇 Full-text search indexes created';
        RAISE NOTICE '⚡ Quick filters added';
    END $$;