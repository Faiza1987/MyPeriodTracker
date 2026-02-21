-- Stores the agent's current state per user:
-- last prediction made, so it survives restarts
CREATE TABLE IF NOT EXISTS user_state (
    user_id           UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    last_predicted_date DATE,
    last_confidence   VARCHAR(20),
    last_reasons      TEXT[],
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);

-- Stores the history of prediction accuracy:
-- predicted vs actual period start dates, used for learned confidence
CREATE TABLE IF NOT EXISTS prediction_results (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    predicted_date      DATE NOT NULL,
    actual_date         DATE NOT NULL,
    difference_in_days  INT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_prediction_results_user_id ON prediction_results(user_id);

-- Add symptom columns to cycles if not already present
-- (idempotent: safe to run even if migration was partially applied before)
ALTER TABLE cycles
    ADD COLUMN IF NOT EXISTS flow_intensity  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cervical_mucus  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS stress_level    INT CHECK (stress_level >= 1 AND stress_level <= 5),
    ADD COLUMN IF NOT EXISTS illness         BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS notes           TEXT;