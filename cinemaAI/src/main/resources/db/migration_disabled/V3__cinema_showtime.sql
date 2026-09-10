CREATE TABLE cinemas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cinema_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    room_type VARCHAR(30) NOT NULL,
    row_count INT NOT NULL,
    column_count INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rooms_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas(id)
);

CREATE TABLE seat_rows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    display_order INT NOT NULL,
    start_column INT NOT NULL,
    row_type VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_rows_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT uk_seat_rows_room_label UNIQUE (room_id, row_label)
);

CREATE TABLE seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    seat_row_id BIGINT NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_number INT NOT NULL,
    display_column INT NOT NULL,
    seat_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_seats_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_seats_seat_row FOREIGN KEY (seat_row_id) REFERENCES seat_rows(id),
    CONSTRAINT uk_seats_room_position UNIQUE (room_id, row_label, seat_number)
);

CREATE TABLE showtimes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    base_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_showtimes_movie FOREIGN KEY (movie_id) REFERENCES movies(id),
    CONSTRAINT fk_showtimes_room FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE INDEX idx_showtimes_movie ON showtimes(movie_id);
CREATE INDEX idx_showtimes_room ON showtimes(room_id);
CREATE INDEX idx_showtimes_start_time ON showtimes(start_time);
