IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.actors')
      AND name = 'name'
      AND max_length > 100
)
BEGIN
    DECLARE @actorNameConstraint NVARCHAR(128);

    SELECT TOP 1 @actorNameConstraint = constraint_name
    FROM information_schema.constraint_column_usage
    WHERE table_schema = 'dbo'
      AND table_name = 'actors'
      AND column_name = 'name';

    IF @actorNameConstraint IS NOT NULL
        EXEC('ALTER TABLE dbo.actors DROP CONSTRAINT ' + QUOTENAME(@actorNameConstraint));

    ALTER TABLE dbo.actors ALTER COLUMN name NVARCHAR(50) NOT NULL;
    ALTER TABLE dbo.actors ADD CONSTRAINT uq_actors_name UNIQUE (name);
END;
