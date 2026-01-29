CREATE TABLE intercourse_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id UUID NOT NULL REFERENCES cycles(id) ON DELETE CASCADE,
    event_date DATE NOT NULL,
    protected BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
