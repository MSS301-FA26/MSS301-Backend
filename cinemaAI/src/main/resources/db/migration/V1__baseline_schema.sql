IF
OBJECT_ID('dbo.roles', 'U') IS NULL
CREATE TABLE dbo.roles
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name       NVARCHAR(50) NOT NULL UNIQUE,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.users', 'U') IS NULL
CREATE TABLE dbo.users
(
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    email          NVARCHAR(255) NOT NULL UNIQUE,
    password_hash  NVARCHAR(255) NOT NULL,
    status         NVARCHAR(30) NOT NULL,
    email_verified BIT       NOT NULL DEFAULT 0,
    created_at     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.user_profiles', 'U') IS NULL
CREATE TABLE dbo.user_profiles
(
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id        BIGINT    NOT NULL UNIQUE,
    full_name      NVARCHAR(255) NOT NULL,
    phone          NVARCHAR(20),
    phone_verified BIT       NOT NULL DEFAULT 0,
    created_at     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES dbo.users (id)
);

IF
OBJECT_ID('dbo.user_roles', 'U') IS NULL
CREATE TABLE dbo.user_roles
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    role_id    BIGINT    NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES dbo.roles (id),
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id)
);

IF
OBJECT_ID('dbo.refresh_tokens', 'U') IS NULL
CREATE TABLE dbo.refresh_tokens
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    token      NVARCHAR(500) NOT NULL UNIQUE,
    expires_at DATETIME2 NOT NULL,
    revoked    BIT       NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES dbo.users (id)
);

IF
OBJECT_ID('dbo.password_reset_tokens', 'U') IS NULL
CREATE TABLE dbo.password_reset_tokens
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    token      NVARCHAR(500) NOT NULL UNIQUE,
    purpose    NVARCHAR(30) NOT NULL DEFAULT 'EMAIL_VERIFICATION',
    expires_at DATETIME2 NOT NULL,
    used       BIT       NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES dbo.users (id)
);

IF
OBJECT_ID('dbo.email_verification_tokens', 'U') IS NULL
CREATE TABLE dbo.email_verification_tokens
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    token      NVARCHAR(500) NOT NULL UNIQUE,
    expires_at DATETIME2 NOT NULL,
    used       BIT       NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES dbo.users (id)
);

IF
OBJECT_ID('dbo.pending_registrations', 'U') IS NULL
CREATE TABLE dbo.pending_registrations
(
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    email         NVARCHAR(255) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    full_name     NVARCHAR(255) NOT NULL,
    phone         NVARCHAR(20),
    otp           NVARCHAR(6) NOT NULL,
    expires_at    DATETIME2 NOT NULL,
    created_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.phone_verification_tokens', 'U') IS NULL
CREATE TABLE dbo.phone_verification_tokens
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    otp        NVARCHAR(6) NOT NULL,
    phone      NVARCHAR(20) NOT NULL,
    purpose    NVARCHAR(30) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    used       BIT       NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_phone_verification_tokens_user FOREIGN KEY (user_id) REFERENCES dbo.users (id)
);

IF
OBJECT_ID('dbo.genres', 'U') IS NULL
CREATE TABLE dbo.genres
(
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(1000),
    created_at  DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.movies', 'U') IS NULL
CREATE TABLE dbo.movies
(
    id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    title             NVARCHAR(255) NOT NULL,
    description       NVARCHAR(MAX),
    trailer_url       NVARCHAR(500),
    poster_url        NVARCHAR(500),
    avatar_url        NVARCHAR(500),
    duration_minutes  INT       NOT NULL,
    release_date      DATE,
    end_date          DATE,
    language          NVARCHAR(50),
    subtitle_language NVARCHAR(50),
    status            NVARCHAR(30) NOT NULL,
    age_rating        NVARCHAR(20),
    director          NVARCHAR(255),
    main_actors       NVARCHAR(1000),
    cast_list         NVARCHAR(MAX),
    created_at        DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at        DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.movie_genres', 'U') IS NULL
CREATE TABLE dbo.movie_genres
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    movie_id   BIGINT    NOT NULL,
    genre_id   BIGINT    NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES dbo.movies (id),
    CONSTRAINT fk_movie_genres_genre FOREIGN KEY (genre_id) REFERENCES dbo.genres (id),
    CONSTRAINT uk_movie_genres_movie_genre UNIQUE (movie_id, genre_id)
);

IF
OBJECT_ID('dbo.actors', 'U') IS NULL
CREATE TABLE dbo.actors
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name       NVARCHAR(255) NOT NULL UNIQUE,
    biography  NVARCHAR(1000),
    avatar_url NVARCHAR(500),
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.movie_actors', 'U') IS NULL
CREATE TABLE dbo.movie_actors
(
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    movie_id      BIGINT    NOT NULL,
    actor_id      BIGINT    NOT NULL,
    is_main_actor BIT       NOT NULL DEFAULT 1,
    created_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_movie_actors_movie FOREIGN KEY (movie_id) REFERENCES dbo.movies (id),
    CONSTRAINT fk_movie_actors_actor FOREIGN KEY (actor_id) REFERENCES dbo.actors (id),
    CONSTRAINT uk_movie_actors_movie_actor UNIQUE (movie_id, actor_id)
);


IF
OBJECT_ID('dbo.cinemas', 'U') IS NULL
CREATE TABLE dbo.cinemas
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name       NVARCHAR(255) NOT NULL,
    address    NVARCHAR(500) NOT NULL,
    city       NVARCHAR(100),
    phone      NVARCHAR(20),
    status     NVARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.rooms', 'U') IS NULL
CREATE TABLE dbo.rooms
(
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cinema_id    BIGINT    NOT NULL,
    name         NVARCHAR(100) NOT NULL,
    room_type    NVARCHAR(30) NOT NULL,
    row_count    INT       NOT NULL,
    column_count INT       NOT NULL,
    status       NVARCHAR(30) NOT NULL,
    created_at   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_rooms_cinema FOREIGN KEY (cinema_id) REFERENCES dbo.cinemas (id)
);

IF
OBJECT_ID('dbo.seat_rows', 'U') IS NULL
CREATE TABLE dbo.seat_rows
(
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    room_id       BIGINT    NOT NULL,
    row_label     NVARCHAR(10) NOT NULL,
    display_order INT       NOT NULL,
    start_column  INT       NOT NULL,
    row_type      NVARCHAR(30) NOT NULL,
    created_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_seat_rows_room FOREIGN KEY (room_id) REFERENCES dbo.rooms (id),
    CONSTRAINT uk_seat_rows_room_label UNIQUE (room_id, row_label)
);

IF
OBJECT_ID('dbo.seats', 'U') IS NULL
CREATE TABLE dbo.seats
(
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    room_id        BIGINT    NOT NULL,
    seat_row_id    BIGINT    NOT NULL,
    row_label      NVARCHAR(10) NOT NULL,
    seat_number    INT       NOT NULL,
    display_column INT       NOT NULL,
    seat_type      NVARCHAR(30) NOT NULL,
    status         NVARCHAR(30) NOT NULL,
    created_at     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_seats_room FOREIGN KEY (room_id) REFERENCES dbo.rooms (id),
    CONSTRAINT fk_seats_seat_row FOREIGN KEY (seat_row_id) REFERENCES dbo.seat_rows (id),
    CONSTRAINT uk_seats_room_position UNIQUE (room_id, row_label, seat_number)
);

IF
OBJECT_ID('dbo.showtimes', 'U') IS NULL
CREATE TABLE dbo.showtimes
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    movie_id   BIGINT         NOT NULL,
    room_id    BIGINT         NOT NULL,
    start_time DATETIME2      NOT NULL,
    end_time   DATETIME2      NOT NULL,
    base_price DECIMAL(12, 2) NOT NULL,
    status     NVARCHAR(30) NOT NULL,
    created_at DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_showtimes_movie FOREIGN KEY (movie_id) REFERENCES dbo.movies (id),
    CONSTRAINT fk_showtimes_room FOREIGN KEY (room_id) REFERENCES dbo.rooms (id)
);

IF
OBJECT_ID('dbo.ticket_pricing_rules', 'U') IS NULL
CREATE TABLE dbo.ticket_pricing_rules
(
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    ticket_type NVARCHAR(30) NOT NULL,
    room_type   NVARCHAR(30) NOT NULL,
    weekend     BIT            NOT NULL DEFAULT 0,
    holiday     BIT            NOT NULL DEFAULT 0,
    price       DECIMAL(12, 2) NOT NULL,
    active      BIT            NOT NULL DEFAULT 1,
    created_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.ticket_combos', 'U') IS NULL
CREATE TABLE dbo.ticket_combos
(
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name          NVARCHAR(255) NOT NULL UNIQUE,
    description   NVARCHAR(500),
    adult_count   INT            NOT NULL DEFAULT 0,
    child_count   INT            NOT NULL DEFAULT 0,
    senior_count  INT            NOT NULL DEFAULT 0,
    student_count INT            NOT NULL DEFAULT 0,
    price         DECIMAL(12, 2) NOT NULL,
    active        BIT            NOT NULL DEFAULT 1,
    created_at    DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.bookings', 'U') IS NULL
CREATE TABLE dbo.bookings
(
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    booking_code        NVARCHAR(50) NOT NULL UNIQUE,
    user_id             BIGINT         NOT NULL,
    showtime_id         BIGINT         NOT NULL,
    subtotal            DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status              NVARCHAR(30) NOT NULL,
    hold_expires_at     DATETIME2,
    paid_at             DATETIME2,
    cancelled_at        DATETIME2,
    checked_in_at       DATETIME2,
    refund_requested_at DATETIME2,
    refunded_at         DATETIME2,
    refund_reason       NVARCHAR(500),
    qr_code             NVARCHAR(500),
    created_at          DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT fk_bookings_showtime FOREIGN KEY (showtime_id) REFERENCES dbo.showtimes (id)
);

IF
OBJECT_ID('dbo.booking_seats', 'U') IS NULL
CREATE TABLE dbo.booking_seats
(
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    booking_id  BIGINT         NOT NULL,
    showtime_id BIGINT         NOT NULL,
    seat_id     BIGINT         NOT NULL,
    unit_price  DECIMAL(12, 2) NOT NULL,
    status      NVARCHAR(30) NOT NULL,
    created_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings (id),
    CONSTRAINT fk_booking_seats_showtime FOREIGN KEY (showtime_id) REFERENCES dbo.showtimes (id),
    CONSTRAINT fk_booking_seats_seat FOREIGN KEY (seat_id) REFERENCES dbo.seats (id)
);

IF
OBJECT_ID('dbo.booking_tickets', 'U') IS NULL
CREATE TABLE dbo.booking_tickets
(
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    booking_id  BIGINT         NOT NULL,
    ticket_type NVARCHAR(30) NOT NULL,
    viewer_age  INT            NOT NULL,
    quantity    INT            NOT NULL,
    unit_price  DECIMAL(12, 2) NOT NULL,
    line_total  DECIMAL(12, 2) NOT NULL,
    created_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_booking_tickets_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings (id)
);

IF
OBJECT_ID('dbo.food_items', 'U') IS NULL
CREATE TABLE dbo.food_items
(
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name        NVARCHAR(255) NOT NULL,
    description NVARCHAR(500),
    price       DECIMAL(12, 2) NOT NULL,
    image_url   NVARCHAR(500),
    status      NVARCHAR(30) NOT NULL,
    created_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.food_combos', 'U') IS NULL
CREATE TABLE dbo.food_combos
(
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name        NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    price       DECIMAL(12, 2) NOT NULL,
    image_url   NVARCHAR(500),
    status      NVARCHAR(30) NOT NULL,
    created_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME()
);

IF
OBJECT_ID('dbo.booking_food_items', 'U') IS NULL
CREATE TABLE dbo.booking_food_items
(
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    booking_id    BIGINT         NOT NULL,
    food_item_id  BIGINT,
    food_combo_id BIGINT,
    quantity      INT            NOT NULL,
    unit_price    DECIMAL(12, 2) NOT NULL,
    created_at    DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_booking_food_items_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings (id),
    CONSTRAINT fk_booking_food_items_food_item FOREIGN KEY (food_item_id) REFERENCES dbo.food_items (id),
    CONSTRAINT fk_booking_food_items_food_combo FOREIGN KEY (food_combo_id) REFERENCES dbo.food_combos (id)
);

IF
OBJECT_ID('dbo.payments', 'U') IS NULL
BEGIN
CREATE TABLE dbo.payments
(
    id                    BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    booking_id            BIGINT         NOT NULL,
    provider              NVARCHAR(30) NOT NULL,
    transaction_id        NVARCHAR(100),
    amount                DECIMAL(12, 2) NOT NULL,
    status                NVARCHAR(30) NOT NULL,
    paid_at               DATETIME2,
    callback_payload      NVARCHAR(MAX),
    refund_amount         DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    refunded_at           DATETIME2,
    refund_transaction_no NVARCHAR(100),
    refund_method         NVARCHAR(50),
    created_at            DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at            DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings (id)
);
CREATE INDEX idx_payments_status ON dbo.payments (status);
END

IF
OBJECT_ID('dbo.wishlists', 'U') IS NULL
CREATE TABLE dbo.wishlists
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    movie_id   BIGINT    NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT fk_wishlists_movie FOREIGN KEY (movie_id) REFERENCES dbo.movies (id),
    CONSTRAINT uk_wishlists_user_movie UNIQUE (user_id, movie_id)
);

IF
OBJECT_ID('dbo.loyalty_points', 'U') IS NULL
CREATE TABLE dbo.loyalty_points
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    booking_id BIGINT,
    points     INT       NOT NULL,
    type       NVARCHAR(30) NOT NULL,
    tier       NVARCHAR(30) NOT NULL,
    reason     NVARCHAR(500) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_loyalty_points_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT fk_loyalty_points_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings (id)
);

IF
OBJECT_ID('dbo.notifications', 'U') IS NULL
CREATE TABLE dbo.notifications
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    type       NVARCHAR(30) NOT NULL,
    title      NVARCHAR(255) NOT NULL,
    message    NVARCHAR(1000) NOT NULL,
    read_at    DATETIME2,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES dbo.users (id)
);

IF
OBJECT_ID('dbo.reviews', 'U') IS NULL
CREATE TABLE dbo.reviews
(
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    movie_id   BIGINT    NOT NULL,
    booking_id BIGINT,
    rating     INT       NOT NULL,
    comment    NVARCHAR(2000),
    status     NVARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT fk_reviews_movie FOREIGN KEY (movie_id) REFERENCES dbo.movies (id),
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings (id),
    CONSTRAINT uk_reviews_user_movie UNIQUE (user_id, movie_id)
);

IF
OBJECT_ID('dbo.staff_profiles', 'U') IS NULL
CREATE TABLE dbo.staff_profiles
(
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id       BIGINT    NOT NULL UNIQUE,
    cinema_id     BIGINT,
    employee_code NVARCHAR(50) NOT NULL UNIQUE,
    position      NVARCHAR(100) NOT NULL,
    status        NVARCHAR(30) NOT NULL,
    created_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_staff_profiles_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT fk_staff_profiles_cinema FOREIGN KEY (cinema_id) REFERENCES dbo.cinemas (id)
);

IF
OBJECT_ID('dbo.staff_shifts', 'U') IS NULL
CREATE TABLE dbo.staff_shifts
(
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    staff_profile_id BIGINT    NOT NULL,
    start_time       DATETIME2 NOT NULL,
    end_time         DATETIME2 NOT NULL,
    note             NVARCHAR(500),
    created_at       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_staff_shifts_staff_profile FOREIGN KEY (staff_profile_id) REFERENCES dbo.staff_profiles (id)
);

IF
OBJECT_ID('dbo.audit_logs', 'U') IS NULL
CREATE TABLE dbo.audit_logs
(
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    actor_user_id BIGINT,
    action        NVARCHAR(30) NOT NULL,
    target_type   NVARCHAR(100) NOT NULL,
    target_id     BIGINT,
    detail        NVARCHAR(MAX),
    ip_address    NVARCHAR(100),
    created_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES dbo.users (id)
);

IF
OBJECT_ID('dbo.uploaded_files', 'U') IS NULL
CREATE TABLE dbo.uploaded_files
(
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    original_filename   NVARCHAR(255) NOT NULL,
    stored_filename     NVARCHAR(255) NOT NULL,
    provider            NVARCHAR(100) NOT NULL,
    url                 NVARCHAR(500) NOT NULL,
    mime_type           NVARCHAR(100) NOT NULL,
    file_size           BIGINT    NOT NULL,
    uploaded_by_user_id BIGINT,
    status              NVARCHAR(30) NOT NULL,
    created_at          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_uploaded_files_uploaded_by FOREIGN KEY (uploaded_by_user_id) REFERENCES dbo.users (id)
);

IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_movies_status' AND object_id = OBJECT_ID('dbo.movies'))
CREATE INDEX idx_movies_status ON dbo.movies (status);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_user_profiles_phone' AND object_id = OBJECT_ID('dbo.user_profiles'))
CREATE INDEX idx_user_profiles_phone ON dbo.user_profiles (phone);
IF
COL_LENGTH('dbo.email_verification_tokens', 'purpose') IS NULL
ALTER TABLE dbo.email_verification_tokens
    ADD purpose NVARCHAR(30) NOT NULL DEFAULT 'EMAIL_VERIFICATION';
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_movies_release_date' AND object_id = OBJECT_ID('dbo.movies'))
CREATE INDEX idx_movies_release_date ON dbo.movies (release_date);
IF
COL_LENGTH('dbo.movies', 'end_date') IS NULL
ALTER TABLE dbo.movies
    ADD end_date DATE;
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_movies_end_date' AND object_id = OBJECT_ID('dbo.movies'))
CREATE INDEX idx_movies_end_date ON dbo.movies (end_date);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_movie_actors_movie' AND object_id = OBJECT_ID('dbo.movie_actors'))
CREATE INDEX idx_movie_actors_movie ON dbo.movie_actors (movie_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_movie_actors_actor' AND object_id = OBJECT_ID('dbo.movie_actors'))
CREATE INDEX idx_movie_actors_actor ON dbo.movie_actors (actor_id);

IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_showtimes_movie' AND object_id = OBJECT_ID('dbo.showtimes'))
CREATE INDEX idx_showtimes_movie ON dbo.showtimes (movie_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_showtimes_room' AND object_id = OBJECT_ID('dbo.showtimes'))
CREATE INDEX idx_showtimes_room ON dbo.showtimes (room_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_showtimes_start_time' AND object_id = OBJECT_ID('dbo.showtimes'))
CREATE INDEX idx_showtimes_start_time ON dbo.showtimes (start_time);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_ticket_pricing_rules_lookup' AND object_id = OBJECT_ID('dbo.ticket_pricing_rules'))
CREATE INDEX idx_ticket_pricing_rules_lookup ON dbo.ticket_pricing_rules (ticket_type, room_type, weekend, holiday, active);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_bookings_user' AND object_id = OBJECT_ID('dbo.bookings'))
CREATE INDEX idx_bookings_user ON dbo.bookings (user_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_bookings_showtime' AND object_id = OBJECT_ID('dbo.bookings'))
CREATE INDEX idx_bookings_showtime ON dbo.bookings (showtime_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_bookings_status' AND object_id = OBJECT_ID('dbo.bookings'))
CREATE INDEX idx_bookings_status ON dbo.bookings (status);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_booking_seats_booking' AND object_id = OBJECT_ID('dbo.booking_seats'))
CREATE INDEX idx_booking_seats_booking ON dbo.booking_seats (booking_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_booking_tickets_booking' AND object_id = OBJECT_ID('dbo.booking_tickets'))
CREATE INDEX idx_booking_tickets_booking ON dbo.booking_tickets (booking_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_booking_seats_showtime_seat_status' AND object_id = OBJECT_ID('dbo.booking_seats'))
CREATE INDEX idx_booking_seats_showtime_seat_status ON dbo.booking_seats (showtime_id, seat_id, status);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_booking_food_items_booking' AND object_id = OBJECT_ID('dbo.booking_food_items'))
CREATE INDEX idx_booking_food_items_booking ON dbo.booking_food_items (booking_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_payments_transaction_id_not_null' AND object_id = OBJECT_ID('dbo.payments'))
CREATE UNIQUE INDEX uk_payments_transaction_id_not_null ON dbo.payments (transaction_id) WHERE transaction_id IS NOT NULL;
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_wishlists_user' AND object_id = OBJECT_ID('dbo.wishlists'))
CREATE INDEX idx_wishlists_user ON dbo.wishlists (user_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_wishlists_movie' AND object_id = OBJECT_ID('dbo.wishlists'))
CREATE INDEX idx_wishlists_movie ON dbo.wishlists (movie_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_loyalty_points_user' AND object_id = OBJECT_ID('dbo.loyalty_points'))
CREATE INDEX idx_loyalty_points_user ON dbo.loyalty_points (user_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_notifications_user_read' AND object_id = OBJECT_ID('dbo.notifications'))
CREATE INDEX idx_notifications_user_read ON dbo.notifications (user_id, read_at);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_reviews_movie' AND object_id = OBJECT_ID('dbo.reviews'))
CREATE INDEX idx_reviews_movie ON dbo.reviews (movie_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_reviews_status' AND object_id = OBJECT_ID('dbo.reviews'))
CREATE INDEX idx_reviews_status ON dbo.reviews (status);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_staff_shifts_staff_time' AND object_id = OBJECT_ID('dbo.staff_shifts'))
CREATE INDEX idx_staff_shifts_staff_time ON dbo.staff_shifts (staff_profile_id, start_time, end_time);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_audit_logs_actor' AND object_id = OBJECT_ID('dbo.audit_logs'))
CREATE INDEX idx_audit_logs_actor ON dbo.audit_logs (actor_user_id);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_audit_logs_target' AND object_id = OBJECT_ID('dbo.audit_logs'))
CREATE INDEX idx_audit_logs_target ON dbo.audit_logs (target_type, target_id);
