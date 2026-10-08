-- =========================================================================
-- V3__seed_rich_cinema_data.sql
-- Seed dữ liệu phong phú: Thể loại, Diễn viên, Phim, Phòng chiếu, Ghế, Suất chiếu, Bắp nước
-- =========================================================================

-- 1. THỂ LOẠI (GENRES)
INSERT INTO genres(name, description, created_at, updated_at)
SELECT 'Hành Động', 'Những pha hành động mãn nhãn, kịch tính và gay cấn từng giây', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Hành Động');

INSERT INTO genres(name, description, created_at, updated_at)
SELECT 'Khoa Học Viễn Tưởng', 'Khám phá tương lai, không gian vũ trụ và công nghệ siêu tưởng', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Khoa Học Viễn Tưởng');

INSERT INTO genres(name, description, created_at, updated_at)
SELECT 'Tâm Lý', 'Những câu chuyện sâu sắc về tâm lý, số phận con người và cuộc sống', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Tâm Lý');

INSERT INTO genres(name, description, created_at, updated_at)
SELECT 'Tình Cảm', 'Những chuyện tình lãng mạn, ngọt ngào và đầy cảm xúc lay động trái tim', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Tình Cảm');

INSERT INTO genres(name, description, created_at, updated_at)
SELECT 'Hài Hước', 'Những tiếng cười sảng khoái và giây phút giải trí thư giãn', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Hài Hước');

INSERT INTO genres(name, description, created_at, updated_at)
SELECT 'Hoạt Hình', 'Phim hoạt hình đồ họa đỉnh cao dành cho mọi lứa tuổi gia đình', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Hoạt Hình');

INSERT INTO genres(name, description, created_at, updated_at)
SELECT 'Kinh Dị', 'Trải nghiệm cảm giác rùng rợn, giật gân và ám ảnh nghẹt thở', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Kinh Dị');

INSERT INTO genres(name, description, created_at, updated_at)
SELECT 'Gia Đình', 'Tình cảm gia đình ấm áp, thiêng liêng và ý nghĩa giáo dục sâu sắc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Gia Đình');

-- 2. DIỄN VIÊN (ACTORS)
INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Timothée Chalamet', 'Nam tài tử người Mỹ gốc Pháp, ngôi sao của Dune, Wonka, Call Me by Your Name', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Timothée Chalamet');

INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Zendaya', 'Nữ diễn viên đoạt giải Emmy người Mỹ, nổi tiếng với Euphoria, Spider-Man, Dune', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Zendaya');

INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Ryan Reynolds', 'Diễn viên người Canada nổi tiếng toàn cầu với vai Deadpool', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Ryan Reynolds');

INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Hugh Jackman', 'Ngôi sao người Úc huyền thoại gắn liền với hình tượng Wolverine', 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Hugh Jackman');

INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Phương Anh Đào', 'Ngọc nữ điện ảnh Việt Nam, nữ chính xuất sắc trong phim Mai và Bằng Chứng Vô Hình', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Phương Anh Đào');

INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Tuấn Trần', 'Nam diễn viên tài năng đoạt nhiều giải thưởng danh giá, nam chính phim Mai và Bố Già', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Tuấn Trần');

INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Cillian Murphy', 'Nam diễn viên đoạt giải Oscar xuất sắc nhất với siêu phẩm Oppenheimer', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Cillian Murphy');

INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Jack Black', 'Nghệ sĩ lồng tiếng huyền thoại của chú gấu Po trong Kung Fu Panda', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Jack Black');

INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Trấn Thành', 'Nghệ sĩ, đạo diễn các bộ phim trăm tỷ đình đám: Bố Già, Nhà Bà Nữ, Mai', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Trấn Thành');

INSERT INTO actors(name, biography, avatar_url, created_at, updated_at)
SELECT 'Lý Hải', 'Đạo diễn, nhà sản xuất thành công rực rỡ với chuỗi thương hiệu Lật Mặt', 'https://images.unsplash.com/photo-1463453091185-61582044d556?w=400&auto=format&fit=crop&q=80', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actors WHERE name = 'Lý Hải');

-- 3. PHIM (MOVIES)
INSERT INTO movies(title, description, trailer_url, poster_url, avatar_url, duration_minutes, release_date, end_date, language, subtitle_language, status, age_rating, director, main_actors, cast_list, created_at, updated_at)
SELECT 'Dune: Part Two',
  'Paul Atreides hợp lực cùng Chani và tộc người Fremen để trả thù những kẻ đã hủy hoại gia đình mình, đồng thời đối mặt với lựa chọn giữa tình yêu và số phận vũ trụ.',
  'https://www.youtube.com/watch?v=Way9Dexny3w',
  'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&auto=format&fit=crop&q=80',
  166, CURRENT_DATE - 15, CURRENT_DATE + 60, 'Tiếng Anh', 'Phụ đề Tiếng Việt', 'NOW_SHOWING', '13+', 'Denis Villeneuve',
  'Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem',
  'Paul Atreides (Timothée Chalamet), Chani (Zendaya), Lady Jessica (Rebecca Ferguson)',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM movies WHERE title = 'Dune: Part Two');

INSERT INTO movies(title, description, trailer_url, poster_url, avatar_url, duration_minutes, release_date, end_date, language, subtitle_language, status, age_rating, director, main_actors, cast_list, created_at, updated_at)
SELECT 'Deadpool & Wolverine',
  'Cặp đôi bất hảo Deadpool và Wolverine tái xuất màn ảnh rộng trong chuyến phiêu lưu đa vũ trụ đầy ắp hành động cháy nổ và tiếng cười sảng khoái.',
  'https://www.youtube.com/watch?v=73_1biulkYk',
  'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=300&auto=format&fit=crop&q=80',
  128, CURRENT_DATE - 10, CURRENT_DATE + 50, 'Tiếng Anh', 'Phụ đề Tiếng Việt', 'NOW_SHOWING', '18+', 'Shawn Levy',
  'Ryan Reynolds, Hugh Jackman, Emma Corrin',
  'Wade Wilson / Deadpool (Ryan Reynolds), Logan / Wolverine (Hugh Jackman)',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM movies WHERE title = 'Deadpool & Wolverine');

INSERT INTO movies(title, description, trailer_url, poster_url, avatar_url, duration_minutes, release_date, end_date, language, subtitle_language, status, age_rating, director, main_actors, cast_list, created_at, updated_at)
SELECT 'Mai',
  'Mai là câu chuyện xoay quanh cuộc đời của người phụ nữ làm nghề massage cùng những định kiến xã hội, trước khi bước ngoặt tình yêu xuất hiện.',
  'https://www.youtube.com/watch?v=E_W2oWkO-9Y',
  'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300&auto=format&fit=crop&q=80',
  131, CURRENT_DATE - 20, CURRENT_DATE + 40, 'Tiếng Việt', 'Phụ đề Tiếng Anh', 'NOW_SHOWING', '18+', 'Trấn Thành',
  'Phương Anh Đào, Tuấn Trần, Trấn Thành, Hồng Đào',
  'Mai (Phương Anh Đào), Sâu (Tuấn Trần), Ông Hoàng (Trấn Thành)',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM movies WHERE title = 'Mai');

INSERT INTO movies(title, description, trailer_url, poster_url, avatar_url, duration_minutes, release_date, end_date, language, subtitle_language, status, age_rating, director, main_actors, cast_list, created_at, updated_at)
SELECT 'Kung Fu Panda 4',
  'Chú gấu trúc Po chuẩn bị trở thành Thủ lĩnh tinh thần và phải tìm kiếm, huấn luyện một Tân Chiến binh Rồng trước sự đe dọa của Tắc Kè Hoa.',
  'https://www.youtube.com/watch?v=_inKs4eeHiI',
  'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300&auto=format&fit=crop&q=80',
  94, CURRENT_DATE - 5, CURRENT_DATE + 45, 'Tiếng Anh', 'Lồng tiếng & Phụ đề Tiếng Việt', 'NOW_SHOWING', 'P', 'Mike Mitchell',
  'Jack Black, Awkwafina, Viola Davis',
  'Po (Jack Black), Zhen (Awkwafina)',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM movies WHERE title = 'Kung Fu Panda 4');

INSERT INTO movies(title, description, trailer_url, poster_url, avatar_url, duration_minutes, release_date, end_date, language, subtitle_language, status, age_rating, director, main_actors, cast_list, created_at, updated_at)
SELECT 'Lật Mặt 7: Một Điều Ước',
  'Câu chuyện cảm động về đại gia đình bà Hai cùng 5 người con trưởng thành, khơi gợi tình mẫu tử thiêng liêng và những giá trị gia đình sâu sắc.',
  'https://www.youtube.com/watch?v=d_2e7XJjV3w',
  'https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1485846234645-a62644f84728?w=300&auto=format&fit=crop&q=80',
  138, CURRENT_DATE - 2, CURRENT_DATE + 60, 'Tiếng Việt', 'Phụ đề Tiếng Anh', 'NOW_SHOWING', 'K', 'Lý Hải',
  'Thanh Hiền, Trương Minh Cường, Đinh Y Nhung',
  'Bà Hai (Thanh Hiền), Hai Khôn (Trương Minh Cường)',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM movies WHERE title = 'Lật Mặt 7: Một Điều Ước');

INSERT INTO movies(title, description, trailer_url, poster_url, avatar_url, duration_minutes, release_date, end_date, language, subtitle_language, status, age_rating, director, main_actors, cast_list, created_at, updated_at)
SELECT 'Oppenheimer',
  'Câu chuyện kịch tính về cuộc đời nhà vật lý J. Robert Oppenheimer, người lãnh đạo Dự án Manhattan phát minh ra bom nguyên tử.',
  'https://www.youtube.com/watch?v=uYPbbksJxIg',
  'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=800&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=300&auto=format&fit=crop&q=80',
  180, CURRENT_DATE - 25, CURRENT_DATE + 35, 'Tiếng Anh', 'Phụ đề Tiếng Việt', 'NOW_SHOWING', '18+', 'Christopher Nolan',
  'Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr.',
  'J. Robert Oppenheimer (Cillian Murphy), Katherine Oppenheimer (Emily Blunt)',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM movies WHERE title = 'Oppenheimer');

INSERT INTO movies(title, description, trailer_url, poster_url, avatar_url, duration_minutes, release_date, end_date, language, subtitle_language, status, age_rating, director, main_actors, cast_list, created_at, updated_at)
SELECT 'Avatar: Fire and Ash',
  'Hành trình tiếp theo trên hành tinh Pandora, Jake Sully và Neytiri đối mặt với Tộc Tro Tàn đầy hiếu chiến và những bí ẩn của vũ trụ.',
  'https://www.youtube.com/watch?v=d9MyW72ELq0',
  'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=300&auto=format&fit=crop&q=80',
  190, CURRENT_DATE + 30, CURRENT_DATE + 120, 'Tiếng Anh', 'Phụ đề Tiếng Việt', 'UPCOMING', '13+', 'James Cameron',
  'Sam Worthington, Zoe Saldana, Sigourney Weaver',
  'Jake Sully (Sam Worthington), Neytiri (Zoe Saldana)',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM movies WHERE title = 'Avatar: Fire and Ash');

-- 4. LIÊN KẾT PHIM - THỂ LOẠI (MOVIE_GENRES)
INSERT INTO movie_genres(movie_id, genre_id, created_at, updated_at)
SELECT m.id, g.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m CROSS JOIN genres g
WHERE ((m.title = 'Dune: Part Two' AND g.name IN ('Khoa Học Viễn Tưởng', 'Hành Động'))
   OR (m.title = 'Deadpool & Wolverine' AND g.name IN ('Hành Động', 'Hài Hước'))
   OR (m.title = 'Mai' AND g.name IN ('Tâm Lý', 'Tình Cảm'))
   OR (m.title = 'Kung Fu Panda 4' AND g.name IN ('Hoạt Hình', 'Gia Đình'))
   OR (m.title = 'Lật Mặt 7: Một Điều Ước' AND g.name IN ('Gia Đình', 'Tâm Lý'))
   OR (m.title = 'Oppenheimer' AND g.name IN ('Tâm Lý', 'Hành Động'))
   OR (m.title = 'Avatar: Fire and Ash' AND g.name IN ('Khoa Học Viễn Tưởng', 'Hành Động')))
  AND NOT EXISTS (SELECT 1 FROM movie_genres mg WHERE mg.movie_id = m.id AND mg.genre_id = g.id);

-- 5. LIÊN KẾT PHIM - DIỄN VIÊN (MOVIE_ACTORS)
INSERT INTO movie_actors(movie_id, actor_id, is_main_actor, created_at, updated_at)
SELECT m.id, a.id, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m CROSS JOIN actors a
WHERE ((m.title = 'Dune: Part Two' AND a.name IN ('Timothée Chalamet', 'Zendaya'))
   OR (m.title = 'Deadpool & Wolverine' AND a.name IN ('Ryan Reynolds', 'Hugh Jackman'))
   OR (m.title = 'Mai' AND a.name IN ('Phương Anh Đào', 'Tuấn Trần', 'Trấn Thành'))
   OR (m.title = 'Kung Fu Panda 4' AND a.name IN ('Jack Black'))
   OR (m.title = 'Lật Mặt 7: Một Điều Ước' AND a.name IN ('Lý Hải'))
   OR (m.title = 'Oppenheimer' AND a.name IN ('Cillian Murphy')))
  AND NOT EXISTS (SELECT 1 FROM movie_actors ma WHERE ma.movie_id = m.id AND ma.actor_id = a.id);

-- 6. PHÒNG CHIẾU (ROOMS)
INSERT INTO rooms(cinema_id, name, room_type, row_count, column_count, status, created_at, updated_at)
SELECT c.id, 'Hall 2 (IMAX Laser)', 'IMAX', 4, 8, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cinemas c WHERE c.name = 'CinemaAI Central'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.cinema_id = c.id AND r.name = 'Hall 2 (IMAX Laser)');

-- Hàng ghế Hall 2
INSERT INTO seat_rows(room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT r.id, 'A', 1, 1, 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM rooms r WHERE r.name = 'Hall 2 (IMAX Laser)'
  AND NOT EXISTS (SELECT 1 FROM seat_rows sr WHERE sr.room_id = r.id AND sr.row_label = 'A');
INSERT INTO seat_rows(room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT r.id, 'B', 2, 1, 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM rooms r WHERE r.name = 'Hall 2 (IMAX Laser)'
  AND NOT EXISTS (SELECT 1 FROM seat_rows sr WHERE sr.room_id = r.id AND sr.row_label = 'B');
INSERT INTO seat_rows(room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT r.id, 'C', 3, 1, 'VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM rooms r WHERE r.name = 'Hall 2 (IMAX Laser)'
  AND NOT EXISTS (SELECT 1 FROM seat_rows sr WHERE sr.room_id = r.id AND sr.row_label = 'C');
INSERT INTO seat_rows(room_id, row_label, display_order, start_column, row_type, created_at, updated_at)
SELECT r.id, 'D', 4, 1, 'COUPLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM rooms r WHERE r.name = 'Hall 2 (IMAX Laser)'
  AND NOT EXISTS (SELECT 1 FROM seat_rows sr WHERE sr.room_id = r.id AND sr.row_label = 'D');

-- Ghế chi tiết Hall 2 (1..8)
INSERT INTO seats(room_id, seat_row_id, row_label, seat_number, display_column, seat_type, status, created_at, updated_at)
SELECT sr.room_id, sr.id, sr.row_label, n.n, n.n, sr.row_type, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seat_rows sr
JOIN rooms r ON sr.room_id = r.id
CROSS JOIN (VALUES(1),(2),(3),(4),(5),(6),(7),(8)) n(n)
WHERE r.name = 'Hall 2 (IMAX Laser)'
  AND NOT EXISTS (SELECT 1 FROM seats s WHERE s.room_id = sr.room_id AND s.row_label = sr.row_label AND s.seat_number = n.n);

-- 7. BẮP NƯỚC (FOOD ITEMS)
INSERT INTO food_items(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Bắp Rang Bơ Phô Mai', 'Bắp rang bơ giòn rụm phủ lớp phô mai cheddar mặn béo đậm đà', 55000, 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE name = 'Bắp Rang Bơ Phô Mai');

INSERT INTO food_items(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Bắp Rang Bơ Caramel', 'Bắp ngào sốt caramel ngọt dịu thơm lừng hương bơ', 55000, 'https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE name = 'Bắp Rang Bơ Caramel');

INSERT INTO food_items(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Bắp Rang Bơ Truyền Thống', 'Hạt bắp nở đều ngọt ngào cổ điển không thể thiếu khi xem phim', 45000, 'https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE name = 'Bắp Rang Bơ Truyền Thống');

INSERT INTO food_items(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Coca-Cola 32oz', 'Ly nước ngọt Coca-Cola mát lạnh sảng khoái đánh tan cơn khát', 35000, 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE name = 'Coca-Cola 32oz');

INSERT INTO food_items(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Sprite 32oz', 'Nước giải khát vị chanh tươi mát bùng nổ sảng khoái', 35000, 'https://images.unsplash.com/photo-1625772299848-391b6a87d7b3?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE name = 'Sprite 32oz');

INSERT INTO food_items(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Fanta Cam 32oz', 'Vị cam ngọt ngào lấp lánh sảng khoái mát lạnh', 35000, 'https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE name = 'Fanta Cam 32oz');

INSERT INTO food_items(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Nước Khoáng Dasani 500ml', 'Nước khoáng tinh khiết thanh mát tốt cho sức khỏe', 20000, 'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE name = 'Nước Khoáng Dasani 500ml');

INSERT INTO food_items(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Xúc Xích Nướng Phô Mai', 'Xúc xích Đức xông khói nhân phô mai tan chảy thơm nức', 45000, 'https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?w=400&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE name = 'Xúc Xích Nướng Phô Mai');

-- 8. COMBO BẮP NƯỚC (FOOD COMBOS)
INSERT INTO food_combos(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Combo Solo Movie', '1 Bắp rang bơ lớn 64oz (tùy chọn vị) + 1 Nước ngọt 32oz sảng khoái', 79000, 'https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=500&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_combos WHERE name = 'Combo Solo Movie');

INSERT INTO food_combos(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Combo Couple Sweet', '1 Bắp rang bơ lớn 64oz + 2 Nước ngọt 32oz mát lạnh dành cho 2 người', 109000, 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=500&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_combos WHERE name = 'Combo Couple Sweet');

INSERT INTO food_combos(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Combo Party Friends', '2 Bắp rang bơ lớn 64oz + 3 Nước ngọt 32oz + 1 Xúc xích nướng phô mai', 189000, 'https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?w=500&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_combos WHERE name = 'Combo Party Friends');

INSERT INTO food_combos(name, description, price, image_url, status, created_at, updated_at)
SELECT 'Combo VIP Deluxe', '1 Bắp phô mai đặc biệt 64oz + 2 Nước ngọt 32oz + 2 Xúc xích nướng cao cấp', 169000, 'https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?w=500&auto=format&fit=crop&q=80', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM food_combos WHERE name = 'Combo VIP Deluxe');

-- 9. SUẤT CHIẾU CHO CÁC PHIM (SHOWTIMES CHO HÔM NAY VÀ CÁC NGÀY TỚI)
-- Suất chiếu Dune: Part Two (Hall 1: 09:00 - 11:46 hôm nay)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '9' HOUR,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '11' HOUR + INTERVAL '46' MINUTE,
 90000, 110000, 200000, 90000, 60000, 70000, 110000, 80000, 90000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Dune: Part Two' AND r.name = 'Hall 1';

-- Suất chiếu Mai (Hall 1: 13:00 - 15:11 hôm nay)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '13' HOUR,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '15' HOUR + INTERVAL '11' MINUTE,
 90000, 110000, 200000, 90000, 60000, 70000, 110000, 80000, 90000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Mai' AND r.name = 'Hall 1';

-- Suất chiếu Deadpool & Wolverine (Hall 1: 16:00 - 18:08 hôm nay)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '16' HOUR,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '18' HOUR + INTERVAL '8' MINUTE,
 95000, 115000, 210000, 95000, 65000, 75000, 115000, 85000, 95000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Deadpool & Wolverine' AND r.name = 'Hall 1';

-- Suất chiếu Kung Fu Panda 4 (Hall 2: 10:00 - 11:34 hôm nay)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '10' HOUR,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '11' HOUR + INTERVAL '34' MINUTE,
 110000, 140000, 240000, 110000, 75000, 85000, 140000, 105000, 115000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Kung Fu Panda 4' AND r.name = 'Hall 2 (IMAX Laser)';

-- Suất chiếu Dune: Part Two (Hall 2: 14:00 - 16:46 hôm nay)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '14' HOUR,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '16' HOUR + INTERVAL '46' MINUTE,
 120000, 150000, 260000, 120000, 80000, 100000, 150000, 110000, 130000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Dune: Part Two' AND r.name = 'Hall 2 (IMAX Laser)';

-- Suất chiếu Oppenheimer (Hall 2: 18:00 - 21:00 hôm nay)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '18' HOUR,
 CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '21' HOUR,
 130000, 160000, 280000, 130000, 90000, 110000, 160000, 120000, 140000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Oppenheimer' AND r.name = 'Hall 2 (IMAX Laser)';

-- Suất chiếu Lật Mặt 7 (Hall 1: 10:00 - 12:18 ngày mai)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '10' HOUR,
 CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '12' HOUR + INTERVAL '18' MINUTE,
 85000, 105000, 190000, 85000, 55000, 65000, 105000, 75000, 85000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Lật Mặt 7: Một Điều Ước' AND r.name = 'Hall 1';

-- Suất chiếu Dune: Part Two (Hall 1: 13:30 - 16:16 ngày mai)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '13' HOUR + INTERVAL '30' MINUTE,
 CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '16' HOUR + INTERVAL '16' MINUTE,
 90000, 110000, 200000, 90000, 60000, 70000, 110000, 80000, 90000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Dune: Part Two' AND r.name = 'Hall 1';

-- Suất chiếu Deadpool & Wolverine (Hall 2: 15:00 - 17:08 ngày mai)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '15' HOUR,
 CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '17' HOUR + INTERVAL '8' MINUTE,
 120000, 150000, 260000, 120000, 80000, 100000, 150000, 110000, 130000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Deadpool & Wolverine' AND r.name = 'Hall 2 (IMAX Laser)';

-- Suất chiếu Mai (Hall 2: 18:30 - 20:41 ngày mai)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '18' HOUR + INTERVAL '30' MINUTE,
 CAST(CURRENT_DATE + 1 AS TIMESTAMP) + INTERVAL '20' HOUR + INTERVAL '41' MINUTE,
 120000, 150000, 260000, 120000, 80000, 100000, 150000, 110000, 130000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Mai' AND r.name = 'Hall 2 (IMAX Laser)';

-- Suất chiếu Lật Mặt 7 (Hall 2: 14:00 - 16:18 ngày kia)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE + 2 AS TIMESTAMP) + INTERVAL '14' HOUR,
 CAST(CURRENT_DATE + 2 AS TIMESTAMP) + INTERVAL '16' HOUR + INTERVAL '18' MINUTE,
 120000, 150000, 260000, 120000, 80000, 100000, 150000, 110000, 130000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Lật Mặt 7: Một Điều Ước' AND r.name = 'Hall 2 (IMAX Laser)';

-- Suất chiếu Kung Fu Panda 4 (Hall 1: 15:30 - 17:04 ngày kia)
INSERT INTO showtimes(movie_id, room_id, start_time, end_time, base_price, vip_price, couple_price,
 adult_standard_price, child_standard_price, student_standard_price, adult_vip_price, child_vip_price, student_vip_price,
 weekend_surcharge, holiday_surcharge, late_night_surcharge_amount, status, created_at, updated_at)
SELECT m.id, r.id,
 CAST(CURRENT_DATE + 2 AS TIMESTAMP) + INTERVAL '15' HOUR + INTERVAL '30' MINUTE,
 CAST(CURRENT_DATE + 2 AS TIMESTAMP) + INTERVAL '17' HOUR + INTERVAL '4' MINUTE,
 85000, 105000, 190000, 85000, 55000, 65000, 105000, 75000, 85000,
 false, false, 20000, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM movies m, rooms r
WHERE m.title = 'Kung Fu Panda 4' AND r.name = 'Hall 1';
