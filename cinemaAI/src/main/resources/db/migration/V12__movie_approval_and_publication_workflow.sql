-- V12: Movie Approval and Publication Workflow
-- Adds approval and publication tracking fields to movies table
-- Creates movie_approval_histories table for auditing approval transitions

ALTER TABLE movies ADD COLUMN IF NOT EXISTS approval_status VARCHAR(30) DEFAULT 'APPROVED' NOT NULL;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS publication_status VARCHAR(30) DEFAULT 'PUBLISHED' NOT NULL;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP(6) WITHOUT TIME ZONE;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS submitted_by BIGINT REFERENCES users(id);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP(6) WITHOUT TIME ZONE;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS approved_by BIGINT REFERENCES users(id);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP(6) WITHOUT TIME ZONE;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS rejected_by BIGINT REFERENCES users(id);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS published_at TIMESTAMP(6) WITHOUT TIME ZONE;

-- Add check constraints
ALTER TABLE movies DROP CONSTRAINT IF EXISTS movies_approval_status_check;
ALTER TABLE movies ADD CONSTRAINT movies_approval_status_check
    CHECK (approval_status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'));

ALTER TABLE movies DROP CONSTRAINT IF EXISTS movies_publication_status_check;
ALTER TABLE movies ADD CONSTRAINT movies_publication_status_check
    CHECK (publication_status IN ('UNPUBLISHED', 'PUBLISHED', 'ARCHIVED'));

-- Create indices
CREATE INDEX IF NOT EXISTS idx_movies_approval_status ON movies(approval_status);
CREATE INDEX IF NOT EXISTS idx_movies_publication_status ON movies(publication_status);
CREATE INDEX IF NOT EXISTS idx_movies_app_pub_status ON movies(approval_status, publication_status);

-- Safe migration for existing data:
-- Any inactive movie is archived
UPDATE movies SET publication_status = 'ARCHIVED' WHERE status = 'INACTIVE' AND publication_status = 'PUBLISHED';
-- Active/showing movies with showtimes remain APPROVED and PUBLISHED
UPDATE movies SET approval_status = 'APPROVED', publication_status = 'PUBLISHED' WHERE status != 'INACTIVE' AND approval_status IS NULL;

-- Create movie_approval_histories table
CREATE TABLE IF NOT EXISTS movie_approval_histories (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    action VARCHAR(30) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    comment TEXT,
    actor_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mah_movie_id ON movie_approval_histories(movie_id);
CREATE INDEX IF NOT EXISTS idx_mah_actor_id ON movie_approval_histories(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_mah_created_at ON movie_approval_histories(created_at);
