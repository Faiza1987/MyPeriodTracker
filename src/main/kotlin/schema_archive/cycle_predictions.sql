CREATE TABLE cycle_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    predicted_start_date DATE NOT NULL,
    confidence VARCHAR(10) NOT NULL, -- LOW | MEDIUM | HIGH

    reasons TEXT[] NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT now()
);
