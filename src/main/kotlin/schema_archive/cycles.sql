CREATE TABLE cycles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_duration INT NOT NULL DEFAULT 7,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Migration to expand cycles table for AI Agent capabilities
ALTER TABLE cycles
ADD COLUMN flow_intensity VARCHAR(20), -- LIGHT, MEDIUM, HEAVY
ADD COLUMN cervical_mucus VARCHAR(20), -- DRY, STICKY, CREAMY, EGG_WHITE
ADD COLUMN stress_level INT CHECK (stress_level >= 1 AND stress_level <= 5),
ADD COLUMN illness BOOLEAN DEFAULT FALSE,
ADD COLUMN notes TEXT;

-- Indexing for performance as the dataset grows
CREATE INDEX idx_cycles_user_start ON cycles(user_id, period_start);