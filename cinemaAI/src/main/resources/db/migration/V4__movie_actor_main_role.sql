IF COL_LENGTH('dbo.movie_actors', 'is_main_actor') IS NULL
ALTER TABLE dbo.movie_actors
ADD is_main_actor BIT NOT NULL
    CONSTRAINT df_movie_actors_is_main_actor DEFAULT 1;
