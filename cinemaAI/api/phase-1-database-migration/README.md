# Phase 1 - Database migration và schema

Phase 1 không có REST API nghiệp vụ riêng. Phase này chuẩn bị database schema,
constraint, index và seed data để các phase Auth, Movie, Cinema, Booking,
Payment, Loyalty, Notification, Review, Audit và Upload có nền dữ liệu thống
nhất.

## 1. Phạm vi Phase 1

- Baseline schema cho toàn bộ bảng chính.
- Migration tăng dần cho thay đổi layout ghế, actor, movie actor và ma trận giá vé.
- Index cho các truy vấn thường dùng: movie status, showtime time, booking status,
  seat locking, payment transaction, notification unread, review status.
- Seed data chạy khi không ở profile `test`.
- Checklist kiểm tra schema qua API public và Postman.

## 2. Cấu hình hiện tại

### Production/local profile
```properties
spring.jpa.hibernate.ddl-auto=update
spring.flyway.enabled=false
```

### Test profile
```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false
```

Ghi chú: thư mục `db/migration` hiện là nguồn schema baseline để đối chiếu và
triển khai thủ công trên SQL Server. Các file SQL đang dùng cú pháp SQL Server
như `OBJECT_ID`, `COL_LENGTH`, `NVARCHAR`, `DATETIME2`.

## 3. Migration inventory

| Version | File | Mục đích |
|---|---|---|
| V1 | `src/main/resources/db/migration/V1__baseline_schema.sql` | Tạo baseline schema, khóa ngoại, unique constraint và index chính. |
| V2 | `src/main/resources/db/migration/V2__seat_rows_layout.sql` | Bổ sung `seat_rows`, `seat_row_id`, `display_column` cho layout ghế. |
| V3 | `src/main/resources/db/migration/V3__actor_name_length.sql` | Chuẩn hóa độ dài `actors.name` và unique constraint. |
| V4 | `src/main/resources/db/migration/V4__movie_actor_main_role.sql` | Bổ sung cờ `movie_actors.is_main_actor`. |
| V5 | `src/main/resources/db/migration/V5__showtime_ticket_price_matrix.sql` | Bổ sung ma trận giá vé theo loại vé và loại ghế cho showtime. |

## 4. Nhóm bảng chính trong baseline

### Auth, user và staff
```text
roles, users, user_profiles, user_roles, refresh_tokens,
password_reset_tokens, email_verification_tokens, pending_registrations,
phone_verification_tokens, staff_profiles, staff_shifts
```

### Movie catalog và AI
```text
genres, movies, movie_genres, actors, movie_actors,
trailer_interactions, user_preference_profiles, user_cohort_preferences,
ai_analyses, ai_emotion_segments
```

### Cinema, room, seat và showtime
```text
cinemas, rooms, seat_rows, seats, showtimes
```

### Booking, ticket, food và payment
```text
ticket_pricing_rules, ticket_combos, bookings, booking_seats,
booking_tickets, food_items, food_combos, booking_food_items,
promotions, booking_promotions, payments
```

### Engagement, operation và storage
```text
wishlists, loyalty_points, notifications, reviews, audit_logs, uploaded_files
```

## 5. Seed data

Seed data nằm trong package `com.sba301.cinemaai.seeder` và chạy qua
`DataInitializer` khi profile không phải `test`.

| Seeder | Order | Dữ liệu |
|---|---:|---|
| `RoleSeeder` | 10 | Tạo toàn bộ role trong `RoleName`. |
| `AdminAccountSeeder` | 15 | Tạo admin mặc định `admin@cinemaai.com`. |
| `GenreSeeder` | 20 | Tạo Action, Drama, Comedy, Horror, Romance, Sci-Fi. |
| `MovieSeeder` | 30 | Tạo phim mẫu và liên kết genre. |
| `CinemaScheduleSeeder` | 40 | Tạo rạp `CineAI Central`, `Room A`, seat rows/seats và showtime mẫu. |

## 6. Kiểm tra app và database đã sẵn sàng

### Tên mô tả API
Gọi endpoint public để kiểm tra backend đã khởi động và dữ liệu nền có thể truy vấn.

### API
```http
GET /api/v1/genres
```

### JSON
```json
{}
```

### Post-response
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Action",
      "description": "Action movies"
    }
  ],
  "message": "Thành công",
  "timestamp": "2026-06-13T20:00:00"
}
```

## 7. Kiểm tra seed movie

### Tên mô tả API
Kiểm tra movie seed và phân trang public movie.

### API
```http
GET /api/v1/movies?size=10
```

### JSON
```json
{}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 0,
    "size": 10,
    "totalItems": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "message": "Thành công",
  "timestamp": "2026-06-13T20:00:00"
}
```

## 8. Kiểm tra seed cinema, room và seat layout

### Tên mô tả API
Kiểm tra seed rạp/phòng để các phase showtime và booking có dữ liệu nền.

### API
```http
GET /api/v1/cinema/rooms
```

### JSON
```json
{}
```

### Post-response
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Room A",
      "rowCount": 5,
      "columnCount": 8
    }
  ],
  "message": "Thành công",
  "timestamp": "2026-06-13T20:00:00"
}
```

## 9. Checklist hoàn tất Phase 1

- Có đủ migration `V1` đến `V5` trong `src/main/resources/db/migration`.
- `V1__baseline_schema.sql` bao phủ các bảng nghiệp vụ chính.
- `V1` có index cho truy vấn movie, showtime, booking, payment, notification và review.
- `V2` chuyển schema ghế sang `seat_rows` và `display_column`.
- `V5` có ma trận giá vé theo `adult/child/student` và `standard/vip/couple`.
- Seed role, admin, genre, movie, cinema, room, seat và showtime chạy ngoài profile `test`.
- API public `/api/v1/genres`, `/api/v1/movies`, `/api/v1/cinema/rooms` truy vấn được dữ liệu nền.
