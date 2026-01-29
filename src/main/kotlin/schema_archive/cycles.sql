CREATE TABLE cycles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_duration INT NOT NULL DEFAULT 7,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
