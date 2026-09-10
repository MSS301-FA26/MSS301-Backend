CREATE TABLE genres (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE movies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    trailer_url VARCHAR(500),
    poster_url VARCHAR(500),
    duration_minutes INT NOT NULL,
    release_date DATE,
    language VARCHAR(50),
    subtitle_language VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    age_rating VARCHAR(20),
    director VARCHAR(255),
    cast_list TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE movie_genres (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies(id),
    CONSTRAINT fk_movie_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(id),
    CONSTRAINT uk_movie_genres_movie_genre UNIQUE (movie_id, genre_id)
);

CREATE TABLE actors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    biography VARCHAR(1000),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE movie_actors (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movie_actors_movie FOREIGN KEY (movie_id) REFERENCES movies(id),
    CONSTRAINT fk_movie_actors_actor FOREIGN KEY (actor_id) REFERENCES actors(id),
    CONSTRAINT uk_movie_actors_movie_actor UNIQUE (movie_id, actor_id)
);

CREATE INDEX idx_movies_status ON movies(status);
CREATE INDEX idx_movies_release_date ON movies(release_date);
CREATE INDEX idx_movie_actors_movie ON movie_actors(movie_id);
CREATE INDEX idx_movie_actors_actor ON movie_actors(actor_id);

INSERT INTO genres (name, description) VALUES ('Action', 'Action movies');
INSERT INTO genres (name, description) VALUES ('Drama', 'Drama movies');
INSERT INTO genres (name, description) VALUES ('Comedy', 'Comedy movies');
INSERT INTO genres (name, description) VALUES ('Horror', 'Horror movies');
INSERT INTO genres (name, description) VALUES ('Romance', 'Romance movies');
INSERT INTO genres (name, description) VALUES ('Sci-Fi', 'Science fiction movies');
