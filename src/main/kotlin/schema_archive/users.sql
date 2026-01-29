CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    average_cycle_length INT NOT NULL DEFAULT 28,
    typical_period_length INT NOT NULL DEFAULT 7,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

