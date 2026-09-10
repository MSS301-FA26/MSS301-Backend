# ROLE-BASED BUSINESS FLOWS

## CinemaAI / CinePremier

> Nguồn tham chiếu: `SRS_CINEMA_SYSTEM_COMPLETE.md`
> Ngày cập nhật: 25/06/2026 (đối chiếu trực tiếp với mã nguồn BE = `be/new`)
> Phạm vi: luồng nghiệp vụ theo role/actor cho mô hình **một rạp duy nhất**, kèm **API thật** của từng luồng.
> Base path mọi API: `/api/v1`.

### Lưu ý quan trọng về phạm vi đã chốt
- **Wallet đã được bỏ hoàn toàn.** Không còn `WalletController`/`WalletService`/entity `Wallet`. Hoàn tiền **không** cộng vào ví; khi ADMIN hủy suất, booking chỉ chuyển `REFUNDED` (đánh dấu trạng thái) + thu hồi loyalty + gửi notification. Việc trả tiền thực tế xử lý ngoài hệ thống.
- **Promotion/khuyến mãi:** ngoài phạm vi, không có API.
- Database chính thức là **PostgreSQL**; MySQL trong `application.properties` chỉ là cấu hình chạy local trên máy dev.

---

## 1. Quy ước actor

| Actor | Ý nghĩa |
|---|---|
| `GUEST` | Chưa đăng nhập — chỉ đọc dữ liệu công khai. |
| `CUSTOMER` | Khách đã đăng nhập (role `CUSTOMER`). |
| `STAFF` | Nhân viên rạp — check-in vé. |
| `ADMIN` | Quản trị danh mục, vận hành, tài khoản. Không check-in. |
| `SYSTEM` | Scheduler, callback thanh toán, cộng/thu hồi loyalty, notification. |
| `VNPAY` / `SMTP` / `CLOUDINARY` | Dịch vụ ngoài. |

RBAC: `/api/v1/admin/**` → ADMIN; `/api/v1/staff/**` → STAFF; còn lại public hoặc CUSTOMER (theo JWT).

---

## 2. Luồng GUEST (public, không cần token)

### GUEST-01 — Đăng ký / đăng nhập / khôi phục mật khẩu
SRS: AUTH-01→07.

| Bước | API |
|---|---|
| Đăng ký | `POST /auth/register` |
| Gửi/nhập OTP email | `POST /auth/verify-email/request` · `POST /auth/verify-email` |
| Đăng nhập email/mật khẩu | `POST /auth/login` |
| Đăng nhập Google (+xác minh) | `POST /auth/google` · `POST /auth/google/verify` |
| Refresh / logout | `POST /auth/refresh` · `POST /auth/logout` |
| Quên mật khẩu | `POST /auth/password-reset/request` · `POST /auth/password-reset/confirm` |

### GUEST-02 — Xem catalog phim / thể loại / diễn viên
SRS: MOV-01,02,05,07.
`GET /movies` · `GET /movies/{movieId}` · `GET /genres` · `GET /genres/{genreId}` · `GET /actors` · `GET /actors/{actorId}` · `GET /actors/{actorId}/movies`

### GUEST-03 — Xem rạp, suất chiếu, seat map, đồ ăn, giá vé
SRS: CIN-01, SHOW-01,02, FOOD-01, PRICE.
`GET /cinema` · `GET /cinema/rooms` · `GET /cinemas/{id}` · `GET /cinemas/{id}/rooms` · `GET /showtimes` · `GET /showtimes/{id}` · `GET /showtimes/{id}/seat-map` · `GET /foods/items` · `GET /foods/combos` · `GET /ticket-pricing/combos` · `POST /ticket-pricing/validate`

### GUEST-04 — Đọc review công khai
SRS: REV-02,04.
`GET /reviews/movies/{movieId}` · `GET /reviews/movies/{movieId}/average-rating`

> Khi GUEST bấm hold ghế/đặt vé → buộc đăng nhập thành CUSTOMER.

---

## 3. Luồng CUSTOMER (cần JWT)

### CUSTOMER-01 — Hồ sơ cá nhân
SRS: AUTH-08,09,10.
`GET /users/me` · `PUT /users/me` · `POST /users/me/avatar` · `POST /users/me/password`

### CUSTOMER-02 — Hold ghế → tạo booking
SRS: BOOK-01→06, PRICE-01→04, FOOD-03.
1. Tải seat map (GUEST-03) → chọn ghế.
2. `POST /bookings/hold` — giữ ghế 2 phút.
3. Chọn loại vé/tuổi/đồ ăn, kiểm giá: `POST /ticket-pricing/validate`.
4. `POST /bookings` — tạo booking từ hold.

⚠️ Chưa có DB lock chống double-booking (BOOK-04).

### CUSTOMER-03 — Thanh toán
SRS: PAY-01→05, TICKET-01.
`POST /payments/vnpay/create` → (callback VNPAY do SYSTEM xử lý) → booking `PAID` + sinh QR.
Dev/demo: `POST /payments/mock`. Tra cứu: `GET /payments/booking/{bookingId}`.

### CUSTOMER-04 — Xem booking / vé
SRS: BOOK-07, TICKET-01.
`GET /bookings` · `GET /bookings/{bookingId}`

### CUSTOMER-05 — Hủy / yêu cầu hoàn tiền
SRS: BOOK-08, PAY-07.
- Hủy trước thanh toán (`HOLDING`/`PENDING_PAYMENT`): `DELETE /bookings/{bookingId}`.
- `POST /bookings/{bookingId}/refund-request` ⚠️ **vẫn tồn tại trong code** dù SRS (PAY-07) yêu cầu bỏ. Chỉ đổi trạng thái `REFUND_REQUESTED`, **không hoàn tiền thật**.

### CUSTOMER-06 — Wishlist
SRS: WISH-01.
`POST /wishlist` · `GET /wishlist` · `DELETE /wishlist/{movieId}`

### CUSTOMER-07 — Loyalty
SRS: LOY-01,02,05.
`GET /loyalty/me` (xem điểm) · `POST /loyalty/me/redeem` (đổi điểm — đã có; 1000 điểm = 1.000đ).
Cộng điểm tự động khi `PAID` do SYSTEM.

### CUSTOMER-08 — Review phim
SRS: REV-01.
`POST /reviews/movies/{movieId}` (chỉ khi có booking `USED`) · `PUT /reviews/{reviewId}` · `DELETE /reviews/{reviewId}`

### CUSTOMER-09 — Recommendation (AI rule-based)
SRS: REC-01→04, MOV-09.
`POST /recommendations/trailer-interactions` · `POST /recommendations/preferences/refresh` · `GET /recommendations/preferences/me` · `GET /recommendations/movies` · `GET /recommendations/favorite-actors`

### CUSTOMER-10 — Notification
SRS: NOTI-01,02,04.
`GET /notifications/me` · `GET /notifications/me/unread` · `PATCH /notifications/{id}/read` · `PATCH /notifications/me/read-all`

---

## 4. Luồng STAFF

### STAFF-01 — Đăng nhập
Dùng tài khoản ADMIN cấp (xem ADMIN-01): `POST /auth/login`.

### STAFF-02 — Check-in vé bằng QR
SRS: TICKET-03,04, OPS-01.
`POST /staff/check-in` (chỉ role `STAFF`). Kiểm tra booking `PAID`, chưa dùng → chuyển `USED`.

---

## 5. Luồng ADMIN (cần role ADMIN)

### ADMIN-01 — Tài khoản & STAFF
SRS: AUTH-11,12, OPS-03.
`GET /admin/users` · `GET /admin/users/{userId}` · `POST /admin/users/staff` · `PATCH /admin/users/{userId}/status`

### ADMIN-02 — Phim / thể loại / diễn viên
SRS: MOV-03,04,06,08.
- Phim: `GET /admin/movies` · `POST /admin/movies` · `GET|PUT|DELETE /admin/movies/{movieId}` · `PATCH /admin/movies/{movieId}/status`
- Thể loại: `POST /admin/genres` · `PUT|DELETE /admin/genres/{genreId}`
- Diễn viên: `GET /admin/actors` · `POST /admin/actors` · `PUT|DELETE /admin/actors/{actorId}`

### ADMIN-03 — Rạp duy nhất
SRS: CIN-02→06.
`GET /admin/cinema` · `PUT /admin/cinema` · `PATCH /admin/cinema/status` · (`POST`/`DELETE` tồn tại trong code nhưng nghiệp vụ không dùng — single-cinema).

### ADMIN-04 — Phòng & ghế
SRS: CIN-07→11.
`GET /admin/rooms` · `POST /admin/rooms` · `GET|PUT /admin/rooms/{roomId}` · `PATCH /admin/rooms/{roomId}/status` · `GET /admin/rooms/{roomId}/seats` · `POST /admin/rooms/{roomId}/seats/generate` · `PUT /admin/rooms/{roomId}/seats` · `GET|PUT|DELETE /admin/rooms/seats/{seatId}`

### ADMIN-05 — Suất chiếu (+ hủy do sự cố)
SRS: SHOW-03→08, PRICE-05.
`GET /admin/showtimes` · `POST /admin/showtimes` · `POST /admin/showtimes/bulk` · `GET|PUT|DELETE /admin/showtimes/{id}` · `PATCH /admin/showtimes/{id}/status` · `GET /admin/showtimes/{id}/seat-map`.
**Hủy suất đã bán vé:** booking `PAID` → `REFUNDED` + **thu hồi loyalty** + **notification**. (Không cộng tiền vào ví — wallet đã bỏ.)

### ADMIN-06 — Giá vé
SRS: PRICE-05.
`GET|POST /admin/ticket-pricing/rules` · `PUT|DELETE /admin/ticket-pricing/rules/{ruleId}` · `GET|POST /admin/ticket-pricing/combos` · `PUT|DELETE /admin/ticket-pricing/combos/{comboId}`

### ADMIN-07 — Đồ ăn & combo
SRS: FOOD-02 (FOOD-04 tồn kho chưa làm).
`GET|POST /admin/foods/items` · `GET|POST /admin/foods/combos` · `PUT|DELETE /admin/foods/items/{itemId}` · `PUT|DELETE /admin/foods/combos/{comboId}`

### ADMIN-08 — Booking & refund
SRS: BOOK-08, PAY-07,08.
`GET /admin/bookings` · `GET|DELETE /admin/bookings/{bookingId}` · `POST /admin/bookings/{id}/check-in` · `POST /admin/bookings/{id}/refund-request` · `POST /admin/bookings/{id}/mark-refunded`.
`mark-refunded` chỉ đổi `REFUND_REQUESTED → REFUNDED` (không hoàn tiền thật).

### ADMIN-09 — Review moderation
SRS: REV-03.
`GET /admin/reviews` · `PATCH /admin/reviews/{reviewId}/hide` · `DELETE /admin/reviews/{reviewId}`

### ADMIN-10 — Loyalty quản trị
`POST /admin/loyalty/add` · `POST /admin/loyalty/{userId}/redeem`

### ADMIN-11 — Upload ảnh
SRS: UP-01.
`POST /admin/uploads/images` (Cloudinary).

### ADMIN-12 — Báo cáo
SRS: RPT-01→03 (đã có API).
`GET /admin/reports/revenue` · `GET /admin/reports/top-movies` · `GET /admin/reports/occupancy`

### ADMIN-13 — Notification (tạo thủ công) & debug recommendation
`POST /notifications` · `GET /admin/recommendations/users/{userId}/debug`

---

## 6. Luồng SYSTEM (nền)

| Mã | Việc | Cơ chế |
|---|---|---|
| SYSTEM-01 | Giải phóng hold hết hạn | `SeatHoldCleanupScheduler` → booking/seat `EXPIRED`/`RELEASED` |
| SYSTEM-02 | Cập nhật trạng thái suất | `ShowtimeStatusScheduler` (`SCHEDULED→OPEN→COMPLETED`) |
| SYSTEM-03 | Callback thanh toán | `GET /payments/vnpay/return` · `GET /payments/vnpay/ipn` — verify chữ ký/số tiền, idempotent, set `PAID` + QR + cộng loyalty |
| SYSTEM-04 | Loyalty | Cộng điểm khi `PAID`; thu hồi khi hủy suất |
| SYSTEM-05 | Notification tự động | Một phần (event-driven chưa đầy đủ) |

---

## 7. Dịch vụ ngoài
- **VNPAY:** tạo URL, return/IPN sandbox.
- **SMTP:** OTP đăng ký + reset mật khẩu. (Email vé QR: chưa có.)
- **CLOUDINARY:** lưu ảnh phim/diễn viên/avatar.

---

## 8. Đối chiếu nhanh với SRS (điểm lệch còn lại)
- **Code đã làm nhưng SRS ghi "chưa":** Review (REV), Reports (RPT), Showtime scheduler (SHOW-08), Loyalty redeem (LOY-05), Notification read-all (NOTI-04). → nên cập nhật SRS.
- **SRS mô tả nhưng code không có:** Wallet refund (đã bỏ — gỡ khỏi SRS). (DB: PostgreSQL vẫn là target; MySQL chỉ là config local.)
- **Còn thiếu thật:** ký QR (TICKET-02), DB lock chống double-booking (BOOK-04), Flyway/migration chuẩn, gửi vé QR qua email, FE test.
- **Cần quyết:** có bỏ `POST /bookings/{id}/refund-request` (customer refund) theo SRS PAY-07 hay không.

---

## 9. Kịch bản chạy API theo actor (test cases)

> Quy ước kết quả: `2xx` thành công · `400` sai dữ liệu/nghiệp vụ · `401` thiếu/sai token · `403` sai role · `404` không tồn tại · `409` xung đột.
> Token lấy từ `POST /auth/login` → gắn header `Authorization: Bearer <accessToken>`.

### 9.1 GUEST

| # | Kịch bản | API + payload | Kỳ vọng |
|---|---|---|---|
| G1 | Đăng ký hợp lệ | `POST /auth/register` `{email,password,fullName,phone,birthYear}` | 200, gửi OTP |
| G2 | Đăng ký email trùng | `POST /auth/register` email đã có | 400/409 |
| G3 | Xác minh OTP đúng | `POST /auth/verify-email` `{email,otp}` | 200, tạo CUSTOMER |
| G4 | OTP sai/hết hạn | `POST /auth/verify-email` otp sai | 400 |
| G5 | Login đúng | `POST /auth/login` `{username:email,password}` | 200 + access/refresh token |
| G6 | Login sai mật khẩu | `POST /auth/login` sai pass | 401 |
| G7 | Google login thiếu config | `POST /auth/google` `{credential}` khi chưa set `GOOGLE_CLIENT_ID` | 400 "client id not configured" |
| G8 | Xem phim/suất/seat-map | `GET /movies` · `GET /showtimes/{id}/seat-map` | 200, không cần token |
| G9 | GUEST cố hold ghế | `POST /bookings/hold` không token | 401 |
| G10 | Reset mật khẩu | `POST /auth/password-reset/request` → `.../confirm` | 200 |

### 9.2 CUSTOMER (cần token CUSTOMER)

| # | Kịch bản | API + payload | Kỳ vọng |
|---|---|---|---|
| C1 | Xem hồ sơ | `GET /users/me` | 200 |
| C2 | Đổi mật khẩu sai pass cũ | `POST /users/me/password` | 400 |
| C3 | Hold ghế hợp lệ | `POST /bookings/hold` `{showtimeId,seatIds:[..]}` | 200, ghế `HOLDING` 2 phút |
| C4 | Hold ghế đã bị giữ/bán | `POST /bookings/hold` ghế HOLDING/BOOKED | 400/409 |
| C5 | Tạo booking từ hold | `POST /bookings` `{holdBookingId,tickets:[..],foods:[..],comboId,holiday}` | 200 |
| C6 | Tạo booking sau khi hold hết hạn | `POST /bookings` hold đã EXPIRED | 400 |
| C7 | Sai tuổi vs age rating | `POST /bookings` tuổi không hợp lệ | 400 |
| C8 | Thanh toán VNPay | `POST /payments/vnpay/create` `{bookingId}` | 200 + payUrl |
| C9 | Mock payment (dev) | `POST /payments/mock` `{bookingId}` | 200, booking `PAID` + QR |
| C10 | Xem vé của tôi | `GET /bookings` · `GET /bookings/{id}` | 200 |
| C11 | Hủy booking chưa thanh toán | `DELETE /bookings/{id}` (HOLDING/PENDING) | 200, `CANCELLED` |
| C12 | Hủy booking đã PAID | `DELETE /bookings/{id}` (PAID) | 400 |
| C13 | Truy cập booking người khác | `GET /bookings/{idNgườiKhác}` | 403/404 |
| C14 | Wishlist | `POST /wishlist` `{movieId}` · `GET /wishlist` · `DELETE /wishlist/{movieId}` | 200 |
| C15 | Xem điểm | `GET /loyalty/me` | 200 |
| C16 | Đổi điểm vượt số dư | `POST /loyalty/me/redeem?points=999999` | 400 thiếu điểm |
| C17 | Review khi chưa xem phim | `POST /reviews/movies/{id}` chưa có booking `USED` | 400 |
| C18 | Review hợp lệ | `POST /reviews/movies/{id}` `{rating,comment}` có booking USED | 200 |
| C19 | Recommendation | `GET /recommendations/movies` · `GET /recommendations/favorite-actors` | 200 |
| C20 | Notification | `GET /notifications/me` · `PATCH /notifications/me/read-all` | 200 |
| C21 | Customer gọi API admin | `GET /admin/users` với token CUSTOMER | 403 |

### 9.3 STAFF (cần token STAFF)

| # | Kịch bản | API | Kỳ vọng |
|---|---|---|---|
| S1 | Check-in vé PAID | `POST /staff/check-in` `{qrCode}` | 200, booking `USED` |
| S2 | Check-in vé chưa thanh toán | `POST /staff/check-in` booking chưa PAID | 400 |
| S3 | Check-in lại vé đã USED | `POST /staff/check-in` đã check-in | 400 |
| S4 | QR sai/giả | `POST /staff/check-in` qr không hợp lệ | 400/404 |
| S5 | STAFF gọi API admin | `GET /admin/movies` token STAFF | 403 |

### 9.4 ADMIN (cần token ADMIN)

| # | Kịch bản | API | Kỳ vọng |
|---|---|---|---|
| A1 | Tạo phim hợp lệ | `POST /admin/movies` (title≤50, duration 60–180…) | 200 |
| A2 | Tạo phim vi phạm validate | `POST /admin/movies` title>50 | 400 field-level |
| A3 | Lùi trạng thái phim sai luật | `PATCH /admin/movies/{id}/status` ENDED→UPCOMING | 400 |
| A4 | Cấp tài khoản STAFF | `POST /admin/users/staff` | 200 |
| A5 | Khóa/mở user | `PATCH /admin/users/{id}/status` | 200 |
| A6 | Tạo rạp mới (cấm) | `POST /admin/cinema` | nghiệp vụ không dùng (single-cinema) |
| A7 | Sinh ghế | `POST /admin/rooms/{id}/seats/generate` | 200 |
| A8 | Sửa layout khi có booking | `PUT /admin/rooms/{id}/seats` đang có suất active | 400 |
| A9 | Tạo suất trùng giờ/phòng | `POST /admin/showtimes` xung đột | 400/409 |
| A10 | Tạo bulk suất | `POST /admin/showtimes/bulk` | 200 |
| A11 | Hủy suất đã bán vé | `DELETE /admin/showtimes/{id}` | 200; booking `REFUNDED` + thu hồi loyalty + notification (**không ví**) |
| A12 | Mark refunded sai trạng thái | `POST /admin/bookings/{id}/mark-refunded` khi chưa `REFUND_REQUESTED` | 400 |
| A13 | Ẩn/xóa review | `PATCH /admin/reviews/{id}/hide` · `DELETE /admin/reviews/{id}` | 200 |
| A14 | Báo cáo | `GET /admin/reports/revenue?from=..&to=..` | 200 |
| A15 | Upload ảnh | `POST /admin/uploads/images` (multipart) | 200 + URL Cloudinary |

### 9.5 SYSTEM (tự động, không gọi tay)

| # | Kịch bản | Kích hoạt | Kỳ vọng |
|---|---|---|---|
| Y1 | Giải phóng hold hết hạn | `SeatHoldCleanupScheduler` | hold>2′ → `EXPIRED`/ghế `RELEASED` |
| Y2 | Chuyển trạng thái suất | `ShowtimeStatusScheduler` | `SCHEDULED→OPEN→COMPLETED` |
| Y3 | Callback VNPay hợp lệ | `GET /payments/vnpay/ipn` đúng chữ ký | booking `PAID` + cộng loyalty |
| Y4 | Callback lặp (idempotency) | IPN gọi lại trên booking đã PAID | không xử lý trùng |
| Y5 | Callback sai chữ ký | IPN chữ ký sai | từ chối, không đổi trạng thái |

> Gợi ý chạy: dùng **Swagger** (`/swagger-ui.html`) — login lấy token, bấm *Authorize* dán Bearer, rồi chạy lần lượt theo bảng trên. Hoặc import Postman collection trong `api/`.
