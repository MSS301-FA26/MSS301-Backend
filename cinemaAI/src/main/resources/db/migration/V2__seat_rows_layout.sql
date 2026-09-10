IF OBJECT_ID('dbo.seat_rows', 'U') IS NULL
CREATE TABLE dbo.seat_rows (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    row_label NVARCHAR(10) NOT NULL,
    display_order INT NOT NULL,
    start_column INT NOT NULL,
    row_type NVARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_seat_rows_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id),
    CONSTRAINT uk_seat_rows_room_label UNIQUE (room_id, row_label)
);

IF COL_LENGTH('dbo.seats', 'display_column') IS NULL
ALTER TABLE dbo.seats ADD display_column INT NULL;

IF COL_LENGTH('dbo.seats', 'seat_row_id') IS NULL
ALTER TABLE dbo.seats ADD seat_row_id BIGINT NULL;

UPDATE dbo.seats
SET display_column = seat_number
WHERE display_column IS NULL;

INSERT INTO dbo.seat_rows (room_id, row_label, display_order, start_column, row_type)
SELECT
    grouped.room_id,
    grouped.row_label,
    ROW_NUMBER() OVER (PARTITION BY grouped.room_id ORDER BY grouped.row_label),
    1,
    'STANDARD'
FROM (
    SELECT DISTINCT room_id, row_label
    FROM dbo.seats
) grouped
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.seat_rows existing
    WHERE existing.room_id = grouped.room_id
      AND existing.row_label = grouped.row_label
);

UPDATE seat
SET seat_row_id = seat_row.id
FROM dbo.seats seat
JOIN dbo.seat_rows seat_row
  ON seat_row.room_id = seat.room_id
 AND seat_row.row_label = seat.row_label
WHERE seat.seat_row_id IS NULL;

ALTER TABLE dbo.seats ALTER COLUMN display_column INT NOT NULL;
ALTER TABLE dbo.seats ALTER COLUMN seat_row_id BIGINT NOT NULL;

IF OBJECT_ID('dbo.fk_seats_seat_row', 'F') IS NULL
ALTER TABLE dbo.seats
ADD CONSTRAINT fk_seats_seat_row FOREIGN KEY (seat_row_id) REFERENCES dbo.seat_rows(id);
