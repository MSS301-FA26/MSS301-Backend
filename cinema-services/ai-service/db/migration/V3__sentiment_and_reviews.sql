-- Migration V3: Sentiment Reviews, Consistency Tracking and Experiment Variants

CREATE TABLE IF NOT EXISTS movie_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    rating NUMERIC(3, 1),
    comment TEXT,
    sentiment_label VARCHAR(32) NOT NULL,
    sentiment_score DOUBLE PRECISION NOT NULL,
    feedback_consistency VARCHAR(32) DEFAULT 'CONSISTENT',
    confidence_score DOUBLE PRECISION DEFAULT 1.0,
    aspect_sentiment JSONB DEFAULT '{}'::jsonb,
    event_id VARCHAR(64) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_movie_review UNIQUE (user_id, movie_id)
);

CREATE INDEX IF NOT EXISTS idx_movie_reviews_user ON movie_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_movie_reviews_movie ON movie_reviews(movie_id);
CREATE INDEX IF NOT EXISTS idx_movie_reviews_sentiment ON movie_reviews(sentiment_label);

ALTER TABLE recommendation_sets
    ADD COLUMN IF NOT EXISTS experiment_variant VARCHAR(32) DEFAULT 'CONTROL';
