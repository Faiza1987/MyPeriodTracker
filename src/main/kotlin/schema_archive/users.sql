CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    average_cycle_length INT NOT NULL DEFAULT 28, -- User’s baseline expectation
    typical_period_length INT NOT NULL DEFAULT 7, -- User’s default assumption
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

