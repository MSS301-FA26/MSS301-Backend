-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. User Interactions
CREATE TABLE IF NOT EXISTS user_interactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    rating DOUBLE PRECISION,
    interaction_type VARCHAR(32) NOT NULL DEFAULT 'BOOKING_PAID',
    weight DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_movie UNIQUE (user_id, movie_id)
);

CREATE INDEX IF NOT EXISTS idx_interactions_user ON user_interactions(user_id);
CREATE INDEX IF NOT EXISTS idx_interactions_movie ON user_interactions(movie_id);

-- 2. Movie Metadata & Embeddings Cache
CREATE TABLE IF NOT EXISTS movie_embeddings (
    movie_id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    genres VARCHAR(255)[],
    director VARCHAR(255),
    actors VARCHAR(500)[],
    description TEXT,
    poster_url VARCHAR(500),
    release_year INT,
    status VARCHAR(32) DEFAULT 'NOW_SHOWING',
    dense_vector vector(384),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_movie_embeddings_status ON movie_embeddings(status);
CREATE INDEX IF NOT EXISTS idx_movie_embeddings_hnsw ON movie_embeddings USING hnsw (dense_vector vector_cosine_ops);

-- 3. Chatbot Conversation Sessions
CREATE TABLE IF NOT EXISTS chat_sessions (
    conversation_id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT,
    last_movie_id BIGINT,
    history JSONB DEFAULT '[]'::jsonb,
    last_active TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_sessions_user ON chat_sessions(user_id);

-- 4. Search & Recommendation Telemetry Logs
CREATE TABLE IF NOT EXISTS ai_metrics_log (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(64),
    query_type VARCHAR(32) NOT NULL,
    route_selected VARCHAR(16),
    latency_ms DOUBLE PRECISION NOT NULL,
    was_fallback BOOLEAN DEFAULT FALSE,
    rewritten BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_metrics_query_type ON ai_metrics_log(query_type);
