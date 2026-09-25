-- =========================================================================
-- V5__seed_cinema_rooms_and_pricing.sql
-- Seed rạp chiếu, 3 phòng chiếu có đầy đủ 3 loại ghế (Standard, VIP, Couple)
-- và đồ ăn combo cùng quy tắc giá vé
-- =========================================================================

-- 1. Cập nhật ngày chiếu và trạng thái tất cả phim về NOW_SHOWING để mở bán vé
UPDATE movies
SET status = 'NOW_SHOWING',
    release_date = CURRENT_DATE - 15,
    end_date = CURRENT_DATE + 60,
    updated_at = CURRENT_TIMESTAMP;

-- 2. Thêm rạp CinemaAI Central nếu chưa có
INSERT INTO cinemas (id, name, address, city, phone, status, created_at, updated_at)
SELECT 1, 'CinemaAI Central', '1 Cinema Street, Phường Bến Nghé, Quận 1', 'TP. Hồ Chí Minh', '0900000000', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM cinemas WHERE id = 1);

UPDATE cinemas
SET name = 'CinemaAI Central', address = '1 Cinema Street, Phường Bến Nghé, Quận 1', status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

-- 3. Đảm bảo cấu trúc phòng chiếu với đầy đủ 3 loại ghế (STANDARD, VIP, COUPLE)
-- Phòng 1: Hall 1 (Standard 2D) - 6 hàng x 8 cột
INSERT INTO rooms (id, cinema_id, name, room_type, row_count, column_count, status, created_at, updated_at)
SELECT 1, 1, 'Hall 1 (Standard 2D)', 'STANDARD', 6, 8, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 1);

UPDATE rooms SET name = 'Hall 1 (Standard 2D)', status = 'ACTIVE' WHERE id = 1;

INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 101, 1, 'A', 1, 1, 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 101);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 102, 1, 'B', 2, 1, 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 102);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 103, 1, 'C', 3, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 103);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 104, 1, 'D', 4, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 104);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 105, 1, 'E', 5, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 105);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 106, 1, 'F', 6, 1, 'COUPLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 106);

INSERT INTO seats (room_id, seat_row_id, row_label, seat_number, display_column, seat_type, status, created_at, updated_at)
SELECT sr.room_id, sr.id, sr.row_label, n.n, n.n, sr.row_type, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seat_rows sr
CROSS JOIN (VALUES(1),(2),(3),(4),(5),(6),(7),(8)) n(n)
WHERE sr.room_id = 1
  AND NOT EXISTS (
    SELECT 1 FROM seats s WHERE s.room_id = sr.room_id AND s.row_label = sr.row_label AND s.seat_number = n.n
  );

-- Phòng 2: Hall 2 (IMAX Laser) - 7 hàng x 10 cột
INSERT INTO rooms (id, cinema_id, name, room_type, row_count, column_count, status, created_at, updated_at)
SELECT 2, 1, 'Hall 2 (IMAX Laser)', 'IMAX', 7, 10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 2);

UPDATE rooms SET name = 'Hall 2 (IMAX Laser)', status = 'ACTIVE' WHERE id = 2;

INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 201, 2, 'A', 1, 1, 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 201);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 202, 2, 'B', 2, 1, 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 202);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 203, 2, 'C', 3, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 203);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 204, 2, 'D', 4, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 204);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 205, 2, 'E', 5, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 205);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 206, 2, 'F', 6, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 206);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 207, 2, 'G', 7, 1, 'COUPLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 207);

INSERT INTO seats (room_id, seat_row_id, row_label, seat_number, display_column, seat_type, status, created_at, updated_at)
SELECT sr.room_id, sr.id, sr.row_label, n.n, n.n, sr.row_type, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seat_rows sr
CROSS JOIN (VALUES(1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) n(n)
WHERE sr.room_id = 2
  AND NOT EXISTS (
    SELECT 1 FROM seats s WHERE s.room_id = sr.room_id AND s.row_label = sr.row_label AND s.seat_number = n.n
  );

-- Phòng 3: Hall 3 (VIP Gold Class) - 5 hàng x 8 cột
INSERT INTO rooms (id, cinema_id, name, room_type, row_count, column_count, status, created_at, updated_at)
SELECT 3, 1, 'Hall 3 (VIP Gold Class)', 'VIP', 5, 8, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 3);

UPDATE rooms SET name = 'Hall 3 (VIP Gold Class)', status = 'ACTIVE' WHERE id = 3;

INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 301, 3, 'A', 1, 1, 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 301);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 302, 3, 'B', 2, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 302);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 303, 3, 'C', 3, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 303);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 304, 3, 'D', 4, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 304);
INSERT INTO seat_rows (id, room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT 305, 3, 'E', 5, 1, 'COUPLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seat_rows WHERE id = 305);

INSERT INTO seats (room_id, seat_row_id, row_label, seat_number, display_column, seat_type, status, created_at, updated_at)
SELECT sr.room_id, sr.id, sr.row_label, n.n, n.n, sr.row_type, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seat_rows sr
CROSS JOIN (VALUES(1),(2),(3),(4),(5),(6),(7),(8)) n(n)
WHERE sr.room_id = 3
  AND NOT EXISTS (
    SELECT 1 FROM seats s WHERE s.room_id = sr.room_id AND s.row_label = sr.row_label AND s.seat_number = n.n
  );

-- 4. Bắp nước & Combo
INSERT INTO food_items(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 1, 'Bắp Rang Bơ Phô Mai', 'Bắp rang bơ giòn rụm phủ lớp phô mai cheddar mặn béo đậm đà', 55000, 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE id = 1);

INSERT INTO food_items(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 2, 'Bắp Rang Bơ Caramel', 'Bắp ngào sốt caramel ngọt dịu thơm lừng hương bơ', 55000, 'https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE id = 2);

INSERT INTO food_items(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 3, 'Bắp Rang Bơ Truyền Thống', 'Hạt bắp nở đều ngọt ngào cổ điển không thể thiếu khi xem phim', 45000, 'https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE id = 3);

INSERT INTO food_items(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 4, 'Coca-Cola 32oz', 'Ly nước ngọt Coca-Cola mát lạnh sảng khoái đánh tan cơn khát', 35000, 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE id = 4);

INSERT INTO food_items(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 5, 'Sprite 32oz', 'Nước giải khát vị chanh tươi mát bùng nổ sảng khoái', 35000, 'https://images.unsplash.com/photo-1625772299848-391b6a87d7b3?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE id = 5);

INSERT INTO food_items(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 6, 'Fanta Cam 32oz', 'Vị cam ngọt ngào lấp lánh sảng khoái mát lạnh', 35000, 'https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE id = 6);

INSERT INTO food_items(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 7, 'Nước Khoáng Dasani 500ml', 'Nước khoáng tinh khiết thanh mát tốt cho sức khỏe', 20000, 'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE id = 7);

INSERT INTO food_items(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 8, 'Xúc Xích Nướng Phô Mai', 'Xúc xích Đức xông khói nhân phô mai tan chảy thơm nức', 45000, 'https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE id = 8);

INSERT INTO food_combos(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 1, 'Combo Solo Movie', '1 Bắp rang bơ lớn 64oz (tùy chọn vị) + 1 Nước ngọt 32oz sảng khoái', 79000, 'https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=500&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_combos WHERE id = 1);

INSERT INTO food_combos(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 2, 'Combo Couple Sweet', '1 Bắp rang bơ lớn 64oz + 2 Nước ngọt 32oz mát lạnh dành cho 2 người', 109000, 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=500&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_combos WHERE id = 2);

INSERT INTO food_combos(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 3, 'Combo Party Friends', '2 Bắp rang bơ lớn 64oz + 3 Nước ngọt 32oz + 1 Xúc xích nướng phô mai', 189000, 'https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?w=500&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_combos WHERE id = 3);

INSERT INTO food_combos(id, name, description, price, image_url, status, created_at, updated_at)
SELECT 4, 'Combo VIP Deluxe', '1 Bắp phô mai đặc biệt 64oz + 2 Nước ngọt 32oz + 2 Xúc xích nướng cao cấp', 169000, 'https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?w=500&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_combos WHERE id = 4);

-- 5. Ticket pricing rules
INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'ADULT', 'STANDARD', 'STANDARD', false, false, 90000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'ADULT' AND room_type = 'STANDARD' AND seat_type = 'STANDARD' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'ADULT', 'STANDARD', 'VIP', false, false, 115000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'ADULT' AND room_type = 'STANDARD' AND seat_type = 'VIP' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'ADULT', 'STANDARD', 'COUPLE', false, false, 210000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'ADULT' AND room_type = 'STANDARD' AND seat_type = 'COUPLE' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'CHILD', 'STANDARD', 'STANDARD', false, false, 65000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'CHILD' AND room_type = 'STANDARD' AND seat_type = 'STANDARD' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'CHILD', 'STANDARD', 'VIP', false, false, 85000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'CHILD' AND room_type = 'STANDARD' AND seat_type = 'VIP' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'STUDENT', 'STANDARD', 'STANDARD', false, false, 75000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'STUDENT' AND room_type = 'STANDARD' AND seat_type = 'STANDARD' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'STUDENT', 'STANDARD', 'VIP', false, false, 95000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'STUDENT' AND room_type = 'STANDARD' AND seat_type = 'VIP' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'ADULT', 'IMAX', 'STANDARD', false, false, 120000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'ADULT' AND room_type = 'IMAX' AND seat_type = 'STANDARD' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'ADULT', 'IMAX', 'VIP', false, false, 150000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'ADULT' AND room_type = 'IMAX' AND seat_type = 'VIP' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'ADULT', 'IMAX', 'COUPLE', false, false, 270000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'ADULT' AND room_type = 'IMAX' AND seat_type = 'COUPLE' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'ADULT', 'VIP', 'STANDARD', false, false, 140000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'ADULT' AND room_type = 'VIP' AND seat_type = 'STANDARD' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'ADULT', 'VIP', 'VIP', false, false, 170000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'ADULT' AND room_type = 'VIP' AND seat_type = 'VIP' AND weekend = false AND holiday = false);

INSERT INTO ticket_pricing_rules(ticket_type, room_type, seat_type, weekend, holiday, price, active, created_at, updated_at)
SELECT 'ADULT', 'VIP', 'COUPLE', false, false, 300000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ticket_pricing_rules WHERE ticket_type = 'ADULT' AND room_type = 'VIP' AND seat_type = 'COUPLE' AND weekend = false AND holiday = false);
