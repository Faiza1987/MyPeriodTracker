CREATE TABLE IF NOT EXISTS cycle_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    predicted_start_date DATE NOT NULL,
    confidence VARCHAR(20) NOT NULL,
    reasons TEXT[] NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_predictions_user_id ON cycle_predictions(user_id);