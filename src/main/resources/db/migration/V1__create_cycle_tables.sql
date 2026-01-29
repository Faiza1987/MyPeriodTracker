CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    average_cycle_length INT NOT NULL DEFAULT 28,
    typical_period_length INT NOT NULL DEFAULT 7,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE cycles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_duration INT NOT NULL DEFAULT 7,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE intercourse_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id UUID NOT NULL REFERENCES cycles(id) ON DELETE CASCADE,
    event_date DATE NOT NULL,
    protected BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

