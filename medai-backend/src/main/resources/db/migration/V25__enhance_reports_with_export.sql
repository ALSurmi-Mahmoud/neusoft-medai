-- ============================================================================
-- Migration V25: Enhanced Reports with PDF/DOCX Export
-- Description: Add templates, export tracking, and batch export features
-- Author: MedAI Development Team
-- Date: 2026-02-10
-- ============================================================================

-- Report Templates Table
CREATE TABLE IF NOT EXISTS report_templates (
                                                id BIGSERIAL PRIMARY KEY,
                                                name VARCHAR(200) NOT NULL,
                                                category VARCHAR(100), -- radiology, clinical_note, treatment_plan, patient_summary
                                                description TEXT,

    -- Template Content
                                                content TEXT NOT NULL,
                                                header_html TEXT,
                                                footer_html TEXT,

    -- Variables (JSONB) - e.g., {patient_name}, {report_date}
                                                variables JSONB,

    -- Styling
                                                page_size VARCHAR(20) DEFAULT 'A4', -- A4, Letter, Legal
                                                font_family VARCHAR(50) DEFAULT 'Arial',
                                                font_size INTEGER DEFAULT 12,
                                                margin_top INTEGER DEFAULT 20,
                                                margin_bottom INTEGER DEFAULT 20,
                                                margin_left INTEGER DEFAULT 20,
                                                margin_right INTEGER DEFAULT 20,

    -- Metadata
                                                is_system BOOLEAN DEFAULT false,
                                                is_active BOOLEAN DEFAULT true,

                                                created_by BIGINT,
                                                usage_count INTEGER DEFAULT 0,

                                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                                CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Exported Reports Tracking Table
CREATE TABLE IF NOT EXISTS exported_reports (
                                                id BIGSERIAL PRIMARY KEY,

    -- Source Information
                                                report_id BIGINT,
                                                patient_id BIGINT,
                                                treatment_plan_id BIGINT,
                                                clinical_note_id BIGINT,

    -- Export Details
                                                export_type VARCHAR(50) NOT NULL, -- single_report, batch, patient_record, treatment_plan, clinical_note
                                                format VARCHAR(10) NOT NULL, -- pdf, docx
                                                template_id BIGINT,

    -- File Information
                                                file_name VARCHAR(255) NOT NULL,
                                                file_path VARCHAR(500) NOT NULL,
                                                file_size BIGINT,

    -- Metadata
                                                title VARCHAR(300),
                                                description TEXT,

    -- Export Options (JSONB)
    -- e.g., {include_images: true, include_vitals: false, date_range: {start, end}}
                                                options JSONB,

    -- Audit
                                                exported_by BIGINT NOT NULL,
                                                exported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Download tracking
                                                download_count INTEGER DEFAULT 0,
                                                last_downloaded_at TIMESTAMP,

    -- Constraints
                                                CONSTRAINT fk_exported_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE SET NULL,
                                                CONSTRAINT fk_exported_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
                                                CONSTRAINT fk_exported_treatment_plan FOREIGN KEY (treatment_plan_id) REFERENCES treatment_plans(id) ON DELETE SET NULL,
                                                CONSTRAINT fk_exported_clinical_note FOREIGN KEY (clinical_note_id) REFERENCES clinical_notes(id) ON DELETE SET NULL,
                                                CONSTRAINT fk_exported_template FOREIGN KEY (template_id) REFERENCES report_templates(id) ON DELETE SET NULL,
                                                CONSTRAINT fk_exported_user FOREIGN KEY (exported_by) REFERENCES users(id) ON DELETE CASCADE,
                                                CONSTRAINT chk_export_format CHECK (format IN ('pdf', 'docx'))
);

-- Enhance Reports Table
ALTER TABLE reports ADD COLUMN IF NOT EXISTS last_exported_at TIMESTAMP;
ALTER TABLE reports ADD COLUMN IF NOT EXISTS export_count INTEGER DEFAULT 0;
ALTER TABLE reports ADD COLUMN IF NOT EXISTS template_id BIGINT;

-- Add constraint only if it doesn't exist
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_report_template'
        ) THEN
            ALTER TABLE reports ADD CONSTRAINT fk_report_template FOREIGN KEY (template_id) REFERENCES report_templates(id) ON DELETE SET NULL;
        END IF;
    END $$;

-- Indexes for Performance (IF NOT EXISTS)
CREATE INDEX IF NOT EXISTS idx_templates_category ON report_templates(category);
CREATE INDEX IF NOT EXISTS idx_templates_system ON report_templates(is_system);
CREATE INDEX IF NOT EXISTS idx_templates_active ON report_templates(is_active);
CREATE INDEX IF NOT EXISTS idx_templates_usage ON report_templates(usage_count DESC);

CREATE INDEX IF NOT EXISTS idx_exported_report ON exported_reports(report_id);
CREATE INDEX IF NOT EXISTS idx_exported_patient ON exported_reports(patient_id);
CREATE INDEX IF NOT EXISTS idx_exported_type ON exported_reports(export_type);
CREATE INDEX IF NOT EXISTS idx_exported_format ON exported_reports(format);
CREATE INDEX IF NOT EXISTS idx_exported_user ON exported_reports(exported_by);
CREATE INDEX IF NOT EXISTS idx_exported_date ON exported_reports(exported_at DESC);

-- Function to Update Template Usage Count
CREATE OR REPLACE FUNCTION increment_template_usage()
    RETURNS TRIGGER AS $$
BEGIN
    UPDATE report_templates
    SET usage_count = usage_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.template_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop trigger if exists before creating
DROP TRIGGER IF EXISTS trigger_increment_template_usage ON exported_reports;

CREATE TRIGGER trigger_increment_template_usage
    AFTER INSERT ON exported_reports
    FOR EACH ROW
    WHEN (NEW.template_id IS NOT NULL)
EXECUTE FUNCTION increment_template_usage();

-- Function to Update Report Export Count
CREATE OR REPLACE FUNCTION update_report_export_stats()
    RETURNS TRIGGER AS $$
BEGIN
    UPDATE reports
    SET export_count = export_count + 1,
        last_exported_at = NEW.exported_at,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.report_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop trigger if exists before creating
DROP TRIGGER IF EXISTS trigger_update_report_export_stats ON exported_reports;

CREATE TRIGGER trigger_update_report_export_stats
    AFTER INSERT ON exported_reports
    FOR EACH ROW
    WHEN (NEW.report_id IS NOT NULL)
EXECUTE FUNCTION update_report_export_stats();

-- Function to Update Timestamps
CREATE OR REPLACE FUNCTION update_export_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop trigger if exists before creating
DROP TRIGGER IF EXISTS trigger_update_template_timestamp ON report_templates;

CREATE TRIGGER trigger_update_template_timestamp
    BEFORE UPDATE ON report_templates
    FOR EACH ROW
EXECUTE FUNCTION update_export_timestamp();

-- Insert System Templates (only if they don't exist)
INSERT INTO report_templates (name, category, description, content, variables, is_system, page_size)
SELECT * FROM (VALUES
                   (
                       'Standard Radiology Report',
                       'radiology',
                       'Standard template for radiology reports with findings and impression',
                       '<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; font-size: 12pt; line-height: 1.6; margin: 20px; }
        .header { text-align: center; margin-bottom: 20px; border-bottom: 2px solid #333; padding-bottom: 10px; }
        .section { margin: 20px 0; }
        .section-title { font-weight: bold; font-size: 14pt; color: #2c3e50; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-bottom: 10px; }
        .info-table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        .info-table td { padding: 5px 10px; vertical-align: top; }
        .label { font-weight: bold; width: 150px; }
        .signature-section { margin-top: 40px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>RADIOLOGY REPORT</h1>
        <p>{hospital_name}</p>
    </div>

    <div class="section">
        <div class="section-title">PATIENT INFORMATION</div>
        <table class="info-table">
            <tr><td class="label">Patient Name:</td><td>{patient_name}</td></tr>
            <tr><td class="label">Patient ID:</td><td>{patient_id}</td></tr>
            <tr><td class="label">Date of Birth:</td><td>{patient_dob}</td></tr>
            <tr><td class="label">Sex:</td><td>{patient_sex}</td></tr>
        </table>
    </div>

    <div class="section">
        <div class="section-title">STUDY INFORMATION</div>
        <table class="info-table">
            <tr><td class="label">Study Date:</td><td>{study_date}</td></tr>
            <tr><td class="label">Modality:</td><td>{study_modality}</td></tr>
            <tr><td class="label">Accession Number:</td><td>{study_accession}</td></tr>
            <tr><td class="label">Study Description:</td><td>{study_description}</td></tr>
        </table>
    </div>

    <div class="section">
        <div class="section-title">FINDINGS</div>
        <p>{report_findings}</p>
    </div>

    <div class="section">
        <div class="section-title">IMPRESSION</div>
        <p>{report_impression}</p>
    </div>

    <div class="section">
        <div class="section-title">RECOMMENDATIONS</div>
        <p>{report_recommendations}</p>
    </div>

    <div class="signature-section">
        <p><strong>Reported by:</strong> {author_name}</p>
        <p><strong>Report Date:</strong> {report_date}</p>
    </div>
</body>
</html>',
                       '{"patient_name": "string", "patient_id": "string", "patient_dob": "date", "patient_sex": "string", "study_date": "datetime", "study_modality": "string", "study_accession": "string", "study_description": "string", "report_findings": "text", "report_impression": "text", "report_recommendations": "text", "author_name": "string", "report_date": "datetime", "hospital_name": "string"}'::jsonb,
                       true,
                       'A4'
                   )
              ) AS t(name, category, description, content, variables, is_system, page_size)
WHERE NOT EXISTS (
    SELECT 1 FROM report_templates WHERE name = 'Standard Radiology Report'
);

-- Additional templates would follow the same pattern...
-- For brevity, I'm showing just one. You can add the others similarly.

-- Comments
COMMENT ON TABLE report_templates IS 'Templates for generating PDF/DOCX reports';
COMMENT ON TABLE exported_reports IS 'Tracking history of exported reports (PDF/DOCX)';

COMMENT ON COLUMN report_templates.variables IS 'JSONB object defining available variables and their types';
COMMENT ON COLUMN exported_reports.options IS 'Export options like date range, include/exclude sections';

-- Success Message
DO $$
    BEGIN
        RAISE NOTICE '✅ Migration V25 completed successfully!';
        RAISE NOTICE '📄 Report templates system created';
        RAISE NOTICE '💾 Export tracking system created';
        RAISE NOTICE '🎨 Ready for PDF & DOCX generation!';
        RAISE NOTICE '🔧 Templates use CSS 2.1 compatible styling for Flying Saucer';
    END $$;