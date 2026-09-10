-- PostgreSQL indexes for GET /api/v1/movies performance.
-- Run manually if Flyway is disabled or SQL Server migrations are not used.

CREATE INDEX IF NOT EXISTS idx_movies_release_id
    ON movies (release_date DESC, id ASC);

CREATE INDEX IF NOT EXISTS idx_movies_status_release_id
    ON movies (status, release_date DESC, id ASC);

CREATE INDEX IF NOT EXISTS idx_movie_genres_movie
    ON movie_genres (movie_id);

CREATE INDEX IF NOT EXISTS idx_movie_genres_genre_movie
    ON movie_genres (genre_id, movie_id);

CREATE INDEX IF NOT EXISTS idx_movie_actors_movie
    ON movie_actors (movie_id);

CREATE INDEX IF NOT EXISTS idx_movie_actors_actor_movie
    ON movie_actors (actor_id, movie_id);

CREATE INDEX IF NOT EXISTS idx_movie_actors_movie_main
    ON movie_actors (movie_id, is_main_actor);

-- Optional but strongly recommended for keyword search with LIKE '%keyword%'.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_movies_title_trgm
    ON movies USING gin (lower(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_movies_director_trgm
    ON movies USING gin (lower(director) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_movies_language_trgm
    ON movies USING gin (lower(language) gin_trgm_ops);

ANALYZE movies;
ANALYZE movie_genres;
ANALYZE movie_actors;
