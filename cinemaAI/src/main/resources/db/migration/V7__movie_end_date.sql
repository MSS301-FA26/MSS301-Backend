IF COL_LENGTH('dbo.movies', 'end_date') IS NULL
ALTER TABLE dbo.movies ADD end_date DATE;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_movies_end_date' AND object_id = OBJECT_ID('dbo.movies'))
CREATE INDEX idx_movies_end_date ON dbo.movies(end_date);
