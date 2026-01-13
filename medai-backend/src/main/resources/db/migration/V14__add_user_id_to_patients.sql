ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS user_id bigint;

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_patients_user_id ON patients(user_id);
