CREATE TABLE cycle_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    predicted_start_date DATE NOT NULL,
    confidence VARCHAR(20) NOT NULL, -- HIGH, MEDIUM, LOW
    reasons TEXT[] NOT NULL,         -- Stores the explanation array

    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Indexing for fast retrieval of a user's prediction history
CREATE INDEX idx_predictions_user_id ON cycle_predictions(user_id);
