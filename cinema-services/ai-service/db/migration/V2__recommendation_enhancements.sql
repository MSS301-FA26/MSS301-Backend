-- Migration V2: Recommendation Enhancements & Idempotency Foundation

-- 1. Idempotency Tracking: Stores processed events to prevent duplicate consumption from RabbitMQ
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    source_service VARCHAR(64) DEFAULT 'RABBITMQ',
    payload_hash VARCHAR(64),
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_processed_events_type ON processed_events(event_type);
CREATE INDEX IF NOT EXISTS idx_processed_events_processed_at ON processed_events(processed_at);

-- 2. Extend user_interactions: support Negative Feedback, Event ID, and Feedback Scores
ALTER TABLE user_interactions
    ADD COLUMN IF NOT EXISTS event_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS is_disliked BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS raw_feedback_score DOUBLE PRECISION DEFAULT 0.0;

-- Optimize index for user preference queries (recency ordering and dislike filtering)
CREATE INDEX IF NOT EXISTS idx_user_interactions_active 
    ON user_interactions(user_id, is_disliked, updated_at DESC);

-- 3. Recommendation Sets & Items: Persist generated recommendations for CTR tracking and feedback loops
CREATE TABLE IF NOT EXISTS recommendation_sets (
    set_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    branch_id BIGINT,
    strategy VARCHAR(64) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS recommendation_items (
    id BIGSERIAL PRIMARY KEY,
    set_id BIGINT REFERENCES recommendation_sets(set_id) ON DELETE CASCADE,
    movie_id BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    rank INT NOT NULL,
    source VARCHAR(32) NOT NULL,
    reason VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_rec_sets_user ON recommendation_sets(user_id, generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_rec_items_set ON recommendation_items(set_id);
