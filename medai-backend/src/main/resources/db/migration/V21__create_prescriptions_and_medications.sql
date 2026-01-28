-- V21: Create Prescriptions and Medications Tables
-- Phase 4.3: Smart Prescription System

-- ============================================================================
-- MEDICATIONS TABLE (Drug Library)
-- ============================================================================
CREATE TABLE medications (
                             id BIGSERIAL PRIMARY KEY,

    -- Basic Info
                             name VARCHAR(255) NOT NULL,
                             generic_name VARCHAR(255),
                             brand_name VARCHAR(255),
                             drug_class VARCHAR(100),

    -- Identification
                             ndc_code VARCHAR(50), -- National Drug Code
                             rxcui VARCHAR(50),    -- RxNorm Concept Unique Identifier

    -- Drug Information
                             description TEXT,
                             indications TEXT,
                             contraindications TEXT,
                             side_effects TEXT,
                             warnings TEXT,

    -- Dosage Information
                             default_dosage VARCHAR(100),
                             dosage_forms TEXT, -- JSON: ["tablet", "capsule", "syrup"]
                             strengths TEXT,    -- JSON: ["10mg", "20mg", "50mg"]
                             routes TEXT,       -- JSON: ["oral", "IV", "topical"]

    -- Special Considerations
                             requires_monitoring BOOLEAN DEFAULT false,
                             controlled_substance BOOLEAN DEFAULT false,
                             schedule_class VARCHAR(10), -- DEA Schedule: II, III, IV, V

    -- Interaction Data
                             interaction_warnings TEXT, -- JSON array of drug interactions
                             food_interactions TEXT,
                             alcohol_warning BOOLEAN DEFAULT false,
                             pregnancy_category VARCHAR(5), -- A, B, C, D, X

    -- Age/Condition Restrictions
                             min_age INTEGER, -- Minimum age in years
                             max_age INTEGER, -- Maximum age in years (NULL = no max)
                             renal_adjustment BOOLEAN DEFAULT false,
                             hepatic_adjustment BOOLEAN DEFAULT false,

    -- Search/Display
                             keywords TEXT, -- For search optimization
                             active BOOLEAN DEFAULT true,

    -- Audit
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             created_by BIGINT
);

-- Indexes for medications
CREATE INDEX idx_medications_name ON medications(name);
CREATE INDEX idx_medications_generic_name ON medications(generic_name);
CREATE INDEX idx_medications_drug_class ON medications(drug_class);
CREATE INDEX idx_medications_active ON medications(active);

-- ============================================================================
-- PRESCRIPTIONS TABLE
-- ============================================================================
CREATE TABLE prescriptions (
                               id BIGSERIAL PRIMARY KEY,

    -- Identification
                               prescription_number VARCHAR(50) UNIQUE NOT NULL,

    -- Relationships
                               patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                               doctor_id BIGINT NOT NULL REFERENCES users(id),
                               medication_id BIGINT REFERENCES medications(id),

    -- Manual medication entry (if not in medications table)
                               medication_name VARCHAR(255),

    -- Prescription Details
                               dosage VARCHAR(100) NOT NULL,          -- e.g., "500mg"
                               dosage_form VARCHAR(50) NOT NULL,      -- tablet, capsule, syrup, etc.
                               route VARCHAR(50) NOT NULL,            -- oral, IV, topical, etc.
                               frequency VARCHAR(100) NOT NULL,       -- e.g., "twice daily", "every 6 hours"
                               frequency_code VARCHAR(20),            -- BID, TID, QID, Q6H, etc.
                               duration_days INTEGER,                 -- Treatment duration
                               quantity INTEGER NOT NULL,             -- Total quantity prescribed

    -- Refills
                               refills_allowed INTEGER DEFAULT 0,
                               refills_remaining INTEGER DEFAULT 0,
                               refill_history JSONB,                  -- Track refill dates

    -- Instructions
                               instructions TEXT NOT NULL,            -- e.g., "Take with food"
                               notes TEXT,                            -- Doctor's private notes
                               patient_instructions TEXT,             -- Patient-friendly instructions

    -- Timing
                               timing_morning BOOLEAN DEFAULT false,
                               timing_afternoon BOOLEAN DEFAULT false,
                               timing_evening BOOLEAN DEFAULT false,
                               timing_bedtime BOOLEAN DEFAULT false,
                               timing_as_needed BOOLEAN DEFAULT false,

    -- Status and Dates
                               status VARCHAR(50) DEFAULT 'active',   -- active, completed, cancelled, expired
                               prescribed_date DATE NOT NULL DEFAULT CURRENT_DATE,
                               start_date DATE NOT NULL,
                               end_date DATE,
                               last_refill_date DATE,

    -- Validation and Security
                               verified BOOLEAN DEFAULT false,
                               verification_code VARCHAR(10),         -- PIN for pharmacy verification
                               digital_signature TEXT,                -- Doctor's digital signature
                               qr_code_data TEXT,                     -- Encrypted QR code content

    -- Drug Safety Checks
                               interaction_check_passed BOOLEAN DEFAULT true,
                               interaction_warnings JSONB,            -- Warnings shown when prescribed
                               allergy_override BOOLEAN DEFAULT false,
                               allergy_override_reason TEXT,

    -- Pharmacy Information
                               pharmacy_name VARCHAR(255),
                               pharmacy_phone VARCHAR(20),
                               pharmacy_fax VARCHAR(20),
                               dispensed BOOLEAN DEFAULT false,
                               dispensed_date DATE,
                               dispensed_by VARCHAR(255),

    -- Follow-up
                               requires_followup BOOLEAN DEFAULT false,
                               followup_date DATE,
                               followup_notes TEXT,

    -- Audit
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               cancelled_at TIMESTAMP,
                               cancelled_by BIGINT,
                               cancellation_reason TEXT
);

-- Indexes for prescriptions
CREATE INDEX idx_prescriptions_patient ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_doctor ON prescriptions(doctor_id);
CREATE INDEX idx_prescriptions_medication ON prescriptions(medication_id);
CREATE INDEX idx_prescriptions_status ON prescriptions(status);
CREATE INDEX idx_prescriptions_prescribed_date ON prescriptions(prescribed_date);
CREATE INDEX idx_prescriptions_number ON prescriptions(prescription_number);

-- Composite indexes for common queries
CREATE INDEX idx_prescriptions_patient_status ON prescriptions(patient_id, status);
CREATE INDEX idx_prescriptions_patient_active ON prescriptions(patient_id, status)
    WHERE status = 'active';

-- ============================================================================
-- SEED SAMPLE MEDICATIONS (Common drugs)
-- ============================================================================
INSERT INTO medications (
    name, generic_name, brand_name, drug_class,
    description, default_dosage, dosage_forms, strengths, routes,
    indications, contraindications, side_effects, warnings,
    interaction_warnings, alcohol_warning, pregnancy_category,
    min_age, active
) VALUES
-- Pain Relief
(
    'Ibuprofen', 'Ibuprofen', 'Advil, Motrin', 'NSAID',
    'Nonsteroidal anti-inflammatory drug for pain, fever, and inflammation',
    '400mg every 6-8 hours',
    '["tablet", "capsule", "suspension"]',
    '["200mg", "400mg", "600mg", "800mg"]',
    '["oral"]',
    'Pain relief, fever reduction, inflammation',
    'Active peptic ulcer, severe kidney disease, aspirin allergy',
    'Stomach upset, heartburn, dizziness, rash',
    'May increase risk of heart attack or stroke with long-term use',
    '["aspirin", "warfarin", "lithium", "methotrexate"]',
    true,
    'C',
    6,
    true
),
(
    'Acetaminophen', 'Acetaminophen', 'Tylenol', 'Analgesic',
    'Pain reliever and fever reducer',
    '500mg every 4-6 hours',
    '["tablet", "capsule", "liquid"]',
    '["325mg", "500mg", "650mg"]',
    '["oral"]',
    'Pain relief, fever reduction',
    'Severe liver disease',
    'Rare: rash, liver damage with overdose',
    'Do not exceed 4000mg per day. Avoid alcohol.',
    '["warfarin"]',
    true,
    'B',
    0,
    true
),

-- Antibiotics
(
    'Amoxicillin', 'Amoxicillin', 'Amoxil', 'Antibiotic (Penicillin)',
    'Broad-spectrum antibiotic for bacterial infections',
    '500mg three times daily',
    '["capsule", "tablet", "suspension"]',
    '["250mg", "500mg", "875mg"]',
    '["oral"]',
    'Bacterial infections: respiratory, ear, urinary tract',
    'Penicillin allergy, mononucleosis',
    'Diarrhea, nausea, rash, yeast infection',
    'Complete full course even if feeling better',
    '["methotrexate", "warfarin"]',
    false,
    'B',
    0,
    true
),
(
    'Azithromycin', 'Azithromycin', 'Zithromax, Z-Pack', 'Antibiotic (Macrolide)',
    'Antibiotic for respiratory and other infections',
    '500mg day 1, then 250mg days 2-5',
    '["tablet", "suspension"]',
    '["250mg", "500mg"]',
    '["oral"]',
    'Respiratory infections, skin infections, STIs',
    'History of cholestatic jaundice with azithromycin',
    'Diarrhea, nausea, abdominal pain',
    'May prolong QT interval. Monitor in heart disease.',
    '["warfarin", "digoxin"]',
    false,
    'B',
    6,
    true
),

-- Blood Pressure
(
    'Lisinopril', 'Lisinopril', 'Prinivil, Zestril', 'ACE Inhibitor',
    'Blood pressure medication, protects kidneys',
    '10mg once daily',
    '["tablet"]',
    '["2.5mg", "5mg", "10mg", "20mg", "40mg"]',
    '["oral"]',
    'High blood pressure, heart failure, post-MI',
    'Pregnancy, history of angioedema, bilateral renal artery stenosis',
    'Dizziness, cough, elevated potassium',
    'Can cause birth defects. Do not use in pregnancy.',
    '["potassium supplements", "NSAIDs"]',
    false,
    'D',
    18,
    true
),
(
    'Amlodipine', 'Amlodipine', 'Norvasc', 'Calcium Channel Blocker',
    'Blood pressure and angina medication',
    '5mg once daily',
    '["tablet"]',
    '["2.5mg", "5mg", "10mg"]',
    '["oral"]',
    'High blood pressure, angina',
    'Severe aortic stenosis, cardiogenic shock',
    'Ankle swelling, flushing, dizziness, palpitations',
    'May cause increased angina or MI when starting',
    '["simvastatin"]',
    false,
    'C',
    18,
    true
),

-- Diabetes
(
    'Metformin', 'Metformin', 'Glucophage', 'Antidiabetic (Biguanide)',
    'First-line medication for type 2 diabetes',
    '500mg twice daily with meals',
    '["tablet", "extended-release"]',
    '["500mg", "850mg", "1000mg"]',
    '["oral"]',
    'Type 2 diabetes mellitus',
    'Severe kidney disease (GFR <30), metabolic acidosis',
    'Diarrhea, nausea, abdominal pain, metallic taste',
    'Risk of lactic acidosis. Discontinue before contrast imaging.',
    '["alcohol", "contrast dye"]',
    true,
    'B',
    18,
    true
),

-- Asthma/Allergies
(
    'Albuterol', 'Albuterol', 'Proventil, Ventolin', 'Bronchodilator',
    'Quick-relief inhaler for asthma and COPD',
    '2 puffs every 4-6 hours as needed',
    '["inhaler", "nebulizer solution"]',
    '["90mcg per puff", "2.5mg per nebule"]',
    '["inhalation"]',
    'Asthma, COPD, exercise-induced bronchospasm',
    'Hypersensitivity to albuterol',
    'Tremor, nervousness, rapid heart rate, headache',
    'Overuse may indicate poor asthma control',
    '["beta blockers"]',
    false,
    'C',
    4,
    true
),
(
    'Cetirizine', 'Cetirizine', 'Zyrtec', 'Antihistamine',
    'Allergy relief medication',
    '10mg once daily',
    '["tablet", "liquid"]',
    '["5mg", "10mg"]',
    '["oral"]',
    'Allergic rhinitis, urticaria',
    'Hypersensitivity to cetirizine or hydroxyzine',
    'Drowsiness, dry mouth, fatigue',
    'May cause drowsiness. Avoid alcohol.',
    '["sedatives", "alcohol"]',
    true,
    'B',
    2,
    true
),

-- Mental Health
(
    'Sertraline', 'Sertraline', 'Zoloft', 'SSRI Antidepressant',
    'Antidepressant for depression and anxiety',
    '50mg once daily',
    '["tablet", "oral solution"]',
    '["25mg", "50mg", "100mg"]',
    '["oral"]',
    'Depression, anxiety, OCD, PTSD',
    'Use with MAOIs, pimozide',
    'Nausea, diarrhea, insomnia, sexual dysfunction',
    'May increase suicidal thoughts in young adults. Monitor closely.',
    '["MAOIs", "NSAIDs", "warfarin"]',
    true,
    'C',
    18,
    true
),

-- Stomach/GI
(
    'Omeprazole', 'Omeprazole', 'Prilosec', 'Proton Pump Inhibitor',
    'Reduces stomach acid for GERD and ulcers',
    '20mg once daily before breakfast',
    '["capsule", "tablet"]',
    '["10mg", "20mg", "40mg"]',
    '["oral"]',
    'GERD, peptic ulcer, erosive esophagitis',
    'Hypersensitivity to PPIs',
    'Headache, diarrhea, abdominal pain',
    'Long-term use may increase fracture risk, low magnesium',
    '["clopidogrel", "methotrexate"]',
    false,
    'C',
    1,
    true
);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE medications IS 'Drug library with comprehensive medication information';
COMMENT ON TABLE prescriptions IS 'Patient prescriptions with full tracking and safety features';
COMMENT ON COLUMN prescriptions.prescription_number IS 'Unique prescription identifier for pharmacy';
COMMENT ON COLUMN prescriptions.interaction_warnings IS 'JSON array of warnings shown when prescribed';
COMMENT ON COLUMN prescriptions.qr_code_data IS 'Encrypted data for pharmacy scanning';