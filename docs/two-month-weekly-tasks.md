# CinemaAI — Kế hoạch chi tiết 2 tháng & AI Execution Runbook (16/09 – 11/11)

Tài liệu này định hình toàn bộ lộ trình chuyển đổi từ **Monolith (`cinemaAI`)** sang kiến trúc **Microservices** trong vòng 2 tháng, kết hợp giữa:
1. **Phân công tuần tự cho 6 thành viên trong nhóm** (đáp ứng báo cáo tiến độ và phân công đồ án).
2. **AI Execution Runbook**: Kịch bản bóc tách kỹ thuật chi tiết theo từng Task chuẩn hóa (Input, Source Monolith, Target, Constraints, DoD) để đưa cho AI Agent thực thi lần lượt mà không bị lỗi dính chùm (*tight-coupling*).
3. **Frontend (FE) Test Mapping**: Định rõ làm xong từng Task Backend thì mở màn hình FE nào (`FE/cinepremier/src/pages/`) để kiểm thử ngay được tính năng đó, bảo đảm tính xác thực hai đầu FE-BE.

---

## 1. Mục tiêu kiến trúc & Nguyên tắc cốt lõi

### Luồng nghiệp vụ mục tiêu (Core Flow)
```text
Đăng nhập → Xem phim/Lịch chiếu → Chọn suất → Giữ ghế 3 phút
→ Chọn vé & Bắp nước → Checkout (Lấy giá Catalog & Lưu Snapshot)
→ Thanh toán VNPay → Nhận mã QR → Xem lịch sử đặt vé
→ Bắn Interaction Event → Nhận danh sách phim gợi ý (Recommendation)
```

### 5 Nguyên tắc vàng khi phân rã (Bắt buộc tuân thủ)
1. **Bộ 7 Logical ID liên kết mềm**:
   * `userId` *(Identity)*
   * `movieId`, `showtimeId`, `seatId`, `productId` *(Catalog)*
   * `bookingId` *(Booking - Mã tiến trình chính)*
   * `paymentId` *(Payment)*
2. **Database-per-service (No Cross-Database Foreign Keys)**: Tuyệt đối không tạo Foreign Key giữa database của các service. Các ID giữa service chỉ là Logical ID.
3. **Quyền lực giá thuộc về Catalog (Authoritative Pricing)**: Client (Web/Mobile) **chỉ gửi ID và số lượng**, tuyệt đối không gửi giá tiền hoặc tổng tiền. Catalog tính toán và trả giá có thẩm quyền.
4. **Cơ chế Snapshot bất biến tại Booking**: Booking lưu bản sao bất biến (tên phim, phòng chiếu, nhãn ghế, đơn giá vé, đơn giá bắp nước) để khi Catalog thay đổi giá/tên ở tương lai, dữ liệu lịch sử vé và hóa đơn đã mua không bao giờ bị sai lệch.
5. **Định danh duy nhất `bookingId`**: Toàn bộ luồng mua vé từ giữ ghế, chọn bắp nước, thanh toán đến QR check-in dùng duy nhất `bookingId`. Không sinh thêm `checkoutId` hay `orderId`. `foodOrderId` **chỉ sinh ra** khi khách mua bắp nước lẻ (không kèm vé).

---

## 2. Phân công vai trò trong nhóm (6 người)

| Vai trò | Phụ trách chính | Nhiệm vụ kỹ thuật |
|---|---|---|
| **Người 1 (Leader)** | Architecture, Gateway, Docker, Event Platform | Dựng khung mono-repo, RabbitMQ, Gateway, bảo đảm tính độc lập DB và Outbox pattern. |
| **Người 2** | Identity & Catalog Service | Bóc tách User/Role, Movie/Showtime/Seat/Food/Pricing và xây dựng API `checkout-quote`. |
| **Người 3** | Booking & Payment Service | Bóc tách Booking/Hold 3 phút, Snapshot pattern, VNPay IPN webhook, Refund, Check-in QR. |
| **Người 4** | Stitch Web & Web Frontend | Tích hợp giao diện Web theo chuẩn responsive, countdown 3 phút, checkout và QR. |
| **Người 5** | Stitch Mobile & Flutter | Xây dựng Flutter app bám sát customer flow: Home, Seat Map, Checkout, VNPay deep-link, QR. |
| **Người 6** | Recommendation & Paper | Phát triển `AiService` (FastAPI), lắng nghe event từ RabbitMQ, hybrid filtering và viết paper. |

---

## 3. Cấu trúc Source Code mục tiêu (Mono-repo Multi-service)

```text
MSS301-Backend/
├── services/
│   ├── api-gateway/                 # Spring Cloud Gateway (Port 8080)
│   ├── identity-service/            # Spring Boot (Port 8081, DB: identity_db)
│   ├── catalog-service/             # Spring Boot (Port 8082, DB: catalog_db)
│   ├── booking-service/             # Spring Boot (Port 8083, DB: booking_db)
│   ├── payment-service/             # Spring Boot (Port 8084, DB: payment_db)
│   └── recommendation-service/      # Python FastAPI (Port 8000, DB: recommendation_db)
├── contracts/                        # Chứa OpenAPI specs và Event JSON schemas
│   ├── openapi/
│   └── events/
├── infra/                           # Cấu hình hạ tầng
│   ├── docker-compose.yml           # Chạy PostgreSQL (5 DBs), RabbitMQ, Services
│   └── rabbitmq/
├── docs/                            # Tài liệu kiến trúc và kế hoạch
└── FE/cinepremier/                  # Dự án Frontend Web (React + Vite + TailwindCSS)
    └── src/
        ├── pages/
        │   ├── auth/                # AuthModal.jsx, GooglePasswordSetupPage.jsx
        │   ├── user/                # HomePage, ExplorePage, MovieDetailPage, BookingPage, MyTicketsPage...
        │   ├── staff/               # StaffCheckInPage.jsx
        │   └── admin/               # AdminPage.jsx, panels quản lý Catalog/Cinema/Stats
        ├── services/                # authService, movieService, bookingService, paymentService...
        └── routes/                  # AppRoutes.jsx, ProtectedRoute, AdminRoute, StaffRoute
```

---

## 4. AI EXECUTION RUNBOOK: Thứ tự thực thi lần lượt cho AI Agent

Phần này được thiết kế theo dạng **các Unit Task độc lập**, có input, output, ràng buộc rõ ràng và **kịch bản kiểm thử trực tiếp trên giao diện Frontend (`FE/cinepremier`)** để lập trình viên hoặc AI có thể đối chiếu ngay sau khi làm xong từng task.

```text
[Task BE-01: Scaffolding] ──► [Task BE-02: Identity] ──► [Task BE-03: Catalog Core]
                                                                  │
[Task BE-05: Booking Core] ◄── [Task BE-04: Catalog Quote API] ◄──┘
         │
         ▼
[Task BE-06: Seat Hold Concurrency] ──► [Task BE-07: Booking Snapshot & Checkout]
                                                        │
[Task BE-09: RabbitMQ & Outbox] ◄── [Task BE-08: Payment & VNPay Webhook] ◄─────┘
         │
         ▼
[Task BE-10: API Gateway & Auth Forwarding] ──► [Task BE-11: Recommendation Events]
                                                        │
                                                        ▼
                                       [Task BE-12: End-to-End Verification]
```

---

### Task BE-01: Khởi tạo khung Mono-repo và Hạ tầng Docker Compose
* **Mục tiêu**: Tạo cấu trúc thư mục `services/`, `contracts/`, `infra/` và file `docker-compose.yml` định nghĩa 5 database PostgreSQL độc lập và cụm RabbitMQ.
* **Đầu vào / Tham chiếu**: Cấu trúc project hiện tại.
* **Đầu ra cần tạo**:
  * `infra/docker-compose.yml`:
    * `identity-db` (Port 5431), `catalog-db` (Port 5432), `booking-db` (Port 5433), `payment-db` (Port 5434), `recommendation-db` (Port 5435).
    * `rabbitmq` (Port 5672, Management UI 15672).
  * Khởi tạo skeleton `pom.xml` cho từng service Spring Boot trong `services/`.
* **Ràng buộc**: Từng database phải có username, password và schema riêng; tuyệt đối không dùng chung database.
* **Nghiệm thu Backend**: Chạy `docker compose up -d` thành công, kết nối được cả 5 DB và RabbitMQ UI (truy cập `http://localhost:15672`).
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Cấu hình môi trường FE**:
    * Mở `FE/cinepremier/.env`, kiểm tra biến môi trường:
      ```env
      VITE_API_BASE_URL=http://localhost:8080
      ```
  * **Khởi động FE**:
    * Chạy `npm run dev` trong thư mục `FE/cinepremier` (Web mở tại `http://localhost:5173`).
  * **Kiểm thử kết nối hạ tầng**:
    * Mở trình duyệt kiểm tra Endpoint Gateway: `http://localhost:8080/actuator/health` trả về `{"status":"UP"}`.
    * Giao diện App Shell (`src/routes/AppRoutes.jsx`, `src/layouts/UserLayout.jsx`) tải lên không bị crash hoặc lỗi CORS. Console trình duyệt không có lỗi mạng kết nối tới Gateway.

---

### Task BE-02: Bóc tách Identity & Access Service [ĐÃ HOÀN THÀNH]
* **Mục tiêu**: Tách toàn bộ module xác thực và quản lý người dùng sang `services/identity-service`.
* **Source Monolith**:
  * Entities: `User`, `UserProfile`, `Role`, `UserRole`, `StaffProfile`, `RefreshToken`, `EmailVerificationToken`, `PasswordResetToken`, `PendingRegistration`.
  * Services: `AuthServiceImpl`, `UserServiceImpl`, `RefreshTokenServiceImpl`, `EmailVerificationServiceImpl`, `PasswordResetServiceImpl`, `StaffProfileServiceImpl`.
  * Controllers: `AuthController`, `UserController`, `AdminUserController`, `AdminStaffProfileController`.
* **Đích**: `services/identity-service/` (Port 8081).
* **Ràng buộc**:
  * Độc lập hoàn toàn, không import bất kỳ class nào của Movie hay Booking.
  * Chỉ xuất ra ngoài định danh duy nhất: `userId`.
  * Cung cấp JWT Token có chứa `userId`, `email`, `roles`.
  * Tạo migration Flyway `V1__init_identity.sql`.
* **Nghiệm thu Backend**: Chạy API Register, Login, Refresh Token, Profile qua Postman/cURL trả về 200 OK và sinh JWT hợp lệ.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Modal Đăng ký / Đăng nhập (`src/pages/auth/AuthModal.jsx`)**:
    * **Test Đăng ký tài khoản mới**:
      1. Bấm nút "Đăng nhập / Đăng ký" trên Header thanh điều hướng.
      2. Chọn tab "Đăng ký": Nhập Họ tên, Email, Mật khẩu -> Bấm "Tạo tài khoản" (FE gọi `POST /api/v1/auth/register`).
      3. Nhập mã OTP xác thực email gửi về -> Bấm "Xác nhận OTP" (FE gọi `POST /api/v1/auth/verify-email`).
      4. UI hiển thị thông báo thành công và chuyển sang form Đăng nhập.
    * **Test Đăng nhập**:
      1. Nhập Email & Mật khẩu -> Bấm "Đăng nhập" (FE gọi `POST /api/v1/auth/login`).
      2. Kiểm tra `localStorage`: Lưu trữ đúng `cinepremier_access_token`, `cinepremier_refresh_token`, `cinepremier_user_info`.
      3. Giao diện Header: Icon Đăng nhập biến mất, thay bằng Avatar và Tên người dùng thật.
    * **Test Đăng xuất**:
      1. Bấm vào Avatar góc trên bên phải -> Chọn "Đăng xuất" (FE gọi `POST /api/v1/auth/logout`).
      2. LocalStorage được dọn sạch, giao diện quay về trạng thái khách vãng lai.
    * **Test Tự động làm mới phiên (Silent Refresh Token)**:
      1. Chờ hoặc chỉnh sửa token giả lập hết hạn, thực hiện hành động cần token. Interceptor trong `src/services/authService.js` tự động gọi `POST /api/v1/auth/refresh` và tiếp tục request mà người dùng không bị văng ra ngoài.
  * **Trang Hồ sơ cá nhân (`src/pages/user/ProfilePage.jsx` - Route `/profile`)**:
    * **Xem thông tin**: Truy cập `/profile` -> FE gọi `GET /api/v1/users/me`, hiển thị chính xác Họ tên, Email, Số điện thoại.
    * **Cập nhật hồ sơ**: Chỉnh sửa họ tên, số điện thoại -> Bấm "Lưu thay đổi" (FE gọi `PUT /api/v1/users/me`), thông báo Toast thành công hiển thị.
    * **Đổi mật khẩu**: Nhập mật khẩu hiện tại, mật khẩu mới -> Bấm "Đổi mật khẩu" (FE gọi `POST /api/v1/users/me/password`).

---

### Task BE-03: Bóc tách Catalog Core Service
* **Mục tiêu**: Tách toàn bộ kho dữ liệu Phim, Suất chiếu, Phòng/Ghế, Bắp nước và Giá vé sang `services/catalog-service`.
* **Source Monolith**:
  * Movie: `Movie`, `Genre`, `Actor`, `MovieGenre`, `MovieActor`.
  * Cinema & Seat: `Cinema`, `Room`, `SeatRow`, `Seat`.
  * Showtime: `Showtime`.
  * F&B: `FoodItem`, `FoodCombo`.
  * Pricing: `TicketPricingRule`, `TicketCombo`.
  * Services & Controllers: Toàn bộ controller và service tương ứng cho Movie, Showtime, Cinema, Room, Seat, Food, Pricing.
* **Đích**: `services/catalog-service/` (Port 8082).
* **Ràng buộc**:
  * Xóa bỏ mọi liên kết Foreign Key sang bảng `bookings` hoặc `users`.
  * Quản lý trạng thái vật lý của ghế: `SeatStatus` (`AVAILABLE`, `MAINTENANCE`).
  * Tạo migration Flyway `V1__init_catalog.sql` và seed data danh mục phim, suất chiếu mẫu.
* **Nghiệm thu Backend**: Chạy các API GET Movie List, Movie Detail, Showtimes, Room Layout, F&B List thành công.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Trang chủ (`src/pages/user/HomePage.jsx` - Route `/`)**:
    * Hero Banner: Lướt xem poster các phim nổi bật lấy từ Catalog (`GET /api/v1/movies`). Bấm xem trailer mở Modal phát trailer YouTube mượt mà.
    * Danh mục phim "Đang chiếu" (`NOW_SHOWING`) và "Sắp chiếu" (`UPCOMING`): Hiển thị đầy đủ poster, tên phim, thời lượng, thể loại.
  * **Trang Khám phá phim (`src/pages/user/ExplorePage.jsx` - Route `/movies`)**:
    * Danh sách phim dạng Grid có phân trang (`GET /api/v1/movies?page=0&size=8`).
    * Bộ lọc thể loại: Gọi `GET /api/v1/genres`, click chọn thể loại (Hành động, Kinh dị, Hoạt hình...) danh sách phim tự động lọc theo `genreId`.
    * Ô tìm kiếm: Gõ từ khóa tìm kiếm tên phim theo thời gian thực.
  * **Trang Chi tiết phim (`src/pages/user/MovieDetailPage.jsx` - Route `/movies/:id`)**:
    * Gọi `GET /api/v1/movies/:id`: Hiển thị thông tin đạo diễn, diễn viên (`GET /api/v1/actors`), nhãn độ tuổi (`P, C13, C16, C18`), tóm tắt cốt truyện.
    * Thanh chọn ngày chiếu: Click chọn các ngày trong tuần -> gọi `GET /api/v1/showtimes?movieId=:id&date=...` hiển thị danh sách các khung giờ chiếu (suất 2D, 3D).
  * **Trang Lịch chiếu toàn hệ thống (`src/pages/user/ShowtimesPage.jsx` - Route `/showtimes`)**:
    * Xem toàn bộ lịch chiếu trong ngày phân bổ theo từng rạp/phòng chiếu (`Hall 1, Hall 2`).
  * **Trang Danh mục Bắp nước (`src/pages/user/ConcessionsPage.jsx` - Route `/concessions`)**:
    * Gọi `GET /api/v1/foods/items` và `GET /api/v1/foods/combos`: Hiển thị danh sách món lẻ (Bắp rang bơ, Nước ngọt) và Combo tiết kiệm kèm ảnh và đơn giá Catalog.

---

### Task BE-04: Xây dựng Catalog Authoritative Quote API (`checkout-quote`)
* **Mục tiêu**: Cung cấp 1 API nội bộ duy nhất để Booking Service gọi sang lấy giá thẩm quyền và dữ liệu snapshot.
* **Đích**: Thêm vào `services/catalog-service/`.
* **Chi tiết API**:
  * **Endpoint**: `POST /internal/v1/catalog/checkout-quote`
  * **Input Payload**:
    ```json
    {
      "showtimeId": 101,
      "seatIds": [1, 2],
      "tickets": [{"ticketType": "ADULT", "viewerAge": 22, "quantity": 2}],
      "foods": [{"productId": 10, "isCombo": false, "quantity": 1}]
    }
    ```
  * **Xử lý**:
    * Kiểm tra `Showtime` có tồn tại và đang `OPEN` không.
    * Kiểm tra `seatIds` có thuộc đúng `Room` của `Showtime` không.
    * Tính đơn giá từng ghế/vé (kèm phụ thu cuối tuần, ngày lễ, suất chiếu muộn).
    * Kiểm tra và tính đơn giá các món bắp nước.
  * **Output Payload (Authoritative Quote & Snapshot Data)**:
    ```json
    {
      "quoteId": "quote_uuid_xyz",
      "validUntil": "2026-09-15T15:05:00",
      "showtime": {"movieId": 5, "movieTitle": "Mai", "posterUrl": "...", "cinemaName": "CineAI Central", "roomName": "Hall 1", "startTime": "..."},
      "seats": [{"seatId": 1, "seatLabel": "E05", "seatType": "STANDARD", "unitPrice": 90000.00}],
      "tickets": [{"ticketType": "ADULT", "quantity": 2, "unitPrice": 90000.00, "lineTotal": 180000.00}],
      "foods": [{"productId": 10, "productName": "Bắp ngọt lớn", "unitPrice": 45000.00, "quantity": 1, "lineTotal": 45000.00}],
      "subtotal": 225000.00
    }
    ```
* **Nghiệm thu Backend**: Test case truyền ghế hợp lệ trả về đúng cấu trúc giá; truyền `seatId` không tồn tại trả lỗi 400 rõ ràng.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Màn hình Đặt vé - Bước 2: Chọn Loại Vé & Bắp Nước (`src/pages/user/BookingPage.jsx`)**:
    * **Kiểm tra tính giá vé thẩm quyền**:
      1. Sau khi chọn 2 ghế (1 ghế Standard, 1 ghế VIP), chuyển sang Bước 2.
      2. Chọn phân loại vé: 1 vé Người lớn (`ADULT`), 1 vé Sinh viên (`STUDENT`).
      3. FE gửi kiểm tra giá tới backend (`POST /api/v1/ticket-pricing/validate`):
         * Đơn giá từng ghế hiển thị đúng phụ thu ghế VIP (ví dụ: +15.000đ).
         * Giảm trừ vé Sinh viên hiển thị rõ ràng.
         * Phụ thu suất chiếu cuối tuần / đêm muộn tự động cộng vào tổng tiền.
    * **Kiểm tra tính giá Bắp nước đi kèm**:
      1. Bấm thêm "+" 1 Bắp ngọt lớn (45.000đ) và 2 Nước ngọt (50.000đ).
      2. Tạm tính ở góc dưới màn hình tự động nhảy số: `Tổng tiền = Tiền vé + Tiền bắp nước` chính xác theo từng phép tính của Catalog Quote.
      3. Kiểm tra bảo mật: Client không thể tự can thiệp đơn giá trong form HTML/JS; mọi đơn giá hiển thị đều phản ánh đúng con số do backend phản hồi.

---

### Task BE-05: Bóc tách Booking Core & Thiết kế Snapshot Pattern
* **Mục tiêu**: Tạo `services/booking-service` độc lập, tái cấu trúc toàn bộ Entity để xóa bỏ Foreign Key vật lý sang Catalog/Identity.
* **Source Monolith**: `Booking`, `BookingSeat`, `BookingTicket`, `BookingFoodItem`, `FoodOrder`.
* **Đích**: `services/booking-service/` (Port 8083).
* **Ràng buộc cấu trúc Entity**:
  * `Booking`:
    * `bookingId` (PK, Long).
    * `userId` (Long - Logical ID, không FK sang Identity DB).
    * `showtimeId`, `movieId` (Long - Logical ID).
    * **Snapshot fields**: `movieTitleSnapshot`, `moviePosterSnapshot`, `cinemaNameSnapshot`, `roomNameSnapshot`, `showtimeStartSnapshot`.
    * `subtotal`, `discountAmount`, `totalAmount`.
    * `status`: `HOLDING`, `PENDING_PAYMENT`, `PAID`, `USED`, `CANCELLED`, `EXPIRED`, `REFUNDED`.
    * `holdExpiresAt`: Thời điểm hết hạn giữ ghế (3 phút).
  * `BookingSeat`:
    * `seatId` (Long - Logical ID).
    * `seatLabelSnapshot`, `seatTypeSnapshot`, `unitPriceSnapshot`.
    * `status`: `HOLDING`, `BOOKED`, `CHECKED_IN`, `RELEASED`.
  * `BookingFoodItem`:
    * `productId` (Long - Logical ID).
    * `productNameSnapshot`, `unitPriceSnapshot`, `quantity`, `lineTotal`.
  * `FoodOrder`:
    * `foodOrderId` (PK). Chỉ dùng khi mua đồ ăn riêng lẻ không kèm vé.
* **Nghiệm thu Backend**: Chạy migration Flyway `V1__init_booking.sql` thành công trên `booking_db`.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Trang Vé của tôi (`src/pages/user/MyTicketsPage.jsx` - Route `/tickets` tab Vé)**:
    * FE gọi `GET /api/v1/bookings?size=100` (thông qua `bookingService.getMyBookings(accessToken)`).
    * **Kiểm tra tính toàn vẹn của Snapshot bất biến**:
      1. Từng thẻ vé hiển thị đầy đủ thông tin lưu từ Snapshot: Tên phim (`movieTitleSnapshot`), Phòng chiếu (`roomNameSnapshot`), Thời gian bắt đầu chiếu (`showtimeStartSnapshot`), Nhãn ghế (`seatLabelSnapshot` ví dụ: `E05, E06`), và Đơn giá vé lúc mua.
      2. **Thực nghiệm chứng minh Snapshot**: Vào Catalog sửa tên phim hoặc tăng giá vé thêm 50.000đ -> F5 lại màn hình `/tickets` trên FE, vé cũ của user vẫn giữ nguyên 100% tên phim cũ và số tiền cũ, không bị lỗi sai lệch dữ liệu lịch sử.
  * **Trang Lịch sử Đơn Bắp nước độc lập (`src/pages/user/FoodOrdersHistoryPage.jsx` - Route `/tickets` tab Bắp nước)**:
    * FE gọi `GET /api/v1/food-orders/my` (thông qua `bookingService.getMyFoodOrders(accessToken)`).
    * Kiểm tra hiển thị danh sách đơn F&B mua riêng lẻ với mã đơn dạng `FO-xxxx` (`foodOrderId`), độc lập hoàn toàn với mã đặt vé `BK-xxxx` (`bookingId`).

---

### Task BE-06: Triển khai Giữ ghế (Seat Hold Concurrency) trong Booking Service
* **Mục tiêu**: Cài đặt luồng giữ ghế 3 phút an toàn, ngăn chặn tình trạng 2 người cùng giữ 1 ghế (*Race Condition*).
* **Source Monolith**: Logic kiểm tra ghế trong `BookingServiceImpl.holdSeats()`.
* **Đích**: `services/booking-service/`.
* **Yêu cầu kỹ thuật**:
  * Khi client gọi `POST /api/v1/bookings/hold` với `{ showtimeId, seatIds }`:
    1. Kiểm tra xem có ghế nào đang trong trạng thái `HOLDING` (chưa hết 3 phút) hoặc `BOOKED` cho cùng `showtimeId` không.
    2. Sử dụng Database Row Locking (`SELECT ... FOR UPDATE`) hoặc Redis Key Lock (`SETNX showtime:101:seat:5 EX 180`).
    3. Tạo bản ghi `Booking` với trạng thái `HOLDING` và `holdExpiresAt = now() + 3 minutes`.
    4. Trả về `bookingId` và `holdExpiresAt`.
  * Viết một Scheduled Task (quét mỗi 30 giây): Tìm các Booking `HOLDING` hoặc `PENDING_PAYMENT` có `holdExpiresAt < now()` ➔ Chuyển thành `EXPIRED` và giải phóng ghế sang `RELEASED`.
* **Nghiệm thu Backend**: Giả lập 2 request đồng thời giữ cùng 1 ghế: 1 request thành công 200, 1 request trả về 409 Conflict.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Màn hình Sơ đồ ghế tương tác (`src/pages/user/BookingPage.jsx` - Bước 1: Chọn suất & Sơ đồ ghế)**:
    * **Hiển thị sơ đồ ghế thời gian thực**:
      * FE gọi `GET /api/v1/showtimes/:id/seat-map`. Sơ đồ hiển thị rõ ràng vị trí màn hình, lối đi và từng hàng ghế (A, B, C, D, E, F...).
      * Ghế trống (Trắng/Xám), Ghế VIP (Viền tím), Ghế Đôi (Couple - Hồng), Ghế đã có người giữ/mua (Màu đỏ xám có icon khóa, không thể click).
    * **Test luồng Giữ ghế thành công**:
      * Click chọn ghế trống `E05`, `E06`.
      * Bấm "Tiếp tục" -> FE gọi `POST /api/v1/bookings/hold` gửi kèm `{ showtimeId, seatIds: [...] }`.
      * Nhận response trả về `bookingId` và `holdExpiresAt`.
    * **Test Đồng hồ đếm ngược 3 phút (Countdown Timer)**:
      * Góc trên màn hình xuất hiện thanh đồng hồ đếm lùi `03:00`, `02:59`... chạy từng giây mượt mà.
    * **Test Xung đột giữ ghế đồng thời (Race Condition)**:
      * Mở 2 cửa sổ trình duyệt song song (Cửa sổ 1: User A, Cửa sổ 2: User B ở chế độ Ẩn danh).
      * Cả 2 cùng mở sơ đồ ghế của suất chiếu đó.
      * User A click chọn ghế `E05` trước và bấm Giữ ghế thành công.
      * Ngay sau đó, User B cũng click chọn ghế `E05` và bấm Giữ ghế.
      * **Kết quả quan sát trên FE của User B**: Backend trả về `409 Conflict`. FE bắt lỗi và hiển thị Modal/Toast cảnh báo: *"Ghế này đã được khách hàng khác giữ chỗ. Vui lòng chọn ghế khác."*, ô ghế `E05` lập tức chuyển sang màu đỏ và bị khóa.
    * **Test Tự động nhả ghế khi hết hạn (Hold Expiration Auto-Release)**:
      * User A giữ ghế nhưng để nguyên màn hình không thanh toán quá 3 phút.
      * Khi đồng hồ đếm lùi về `00:00`: FE hiển thị thông báo *"Phiên giữ ghế của bạn đã hết hạn"*, chuyển hướng về trang chọn suất.
      * Phía User B F5 lại trang: Ghế `E05` đã được giải phóng trở lại trạng thái trống màu trắng, User B có thể click chọn bình thường.

---

### Task BE-07: Tích hợp Checkout & Lưu Snapshot bất biến
* **Mục tiêu**: Hoàn thiện API Checkout trong Booking Service bằng cách gọi Catalog Quote API và khóa số tiền.
* **Đích**: `services/booking-service/`.
* **Quy trình xử lý**:
  * **API**: `POST /api/v1/bookings/{bookingId}/checkout` (hoặc `PUT /api/v1/bookings/{bookingId}/items`)
  * **Input**: `{ tickets: [...], foods: [...] }` *(Client chỉ gửi ID + số lượng)*.
  * **Hành động**:
    1. Kiểm tra `bookingId` còn trong hạn `HOLDING` không.
    2. Gọi đồng bộ REST sang Catalog Service: `POST /internal/v1/catalog/checkout-quote`.
    3. Nhận Authoritative Quote: Sao chép toàn bộ metadata vào các trường Snapshot của `Booking`, `BookingSeat`, `BookingTicket`, `BookingFoodItem`.
    4. Tự tính toán `totalAmount` chính thức trên server.
    5. Cập nhật trạng thái `Booking` sang `PENDING_PAYMENT`.
* **Nghiệm thu Backend**: Đổi giá bắp nước hoặc đổi tên phim trong Catalog DB sau khi checkout, dữ liệu trong `booking_db` vẫn giữ nguyên 100% giá trị cũ.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Màn hình Tóm tắt đơn hàng & Xác nhận thanh toán (`src/pages/user/BookingPage.jsx` - Bước 3: Xác nhận & Thanh toán)**:
    * **Kiểm tra thông tin tóm tắt đơn hàng**:
      1. Bảng tóm tắt hiển thị trực quan thông tin vé:
         * Tên phim, Định dạng phòng chiếu, Ngày chiếu & Khung giờ chiếu.
         * Danh sách ghế đã giữ: Vị trí ghế, Loại ghế, Đơn giá server xác nhận.
         * Phân loại vé: Số lượng vé Người lớn/Sinh viên.
         * Danh sách bắp nước đi kèm: Tên món, số lượng, đơn giá.
      2. Dòng "Tổng cộng" (`totalAmount`) được hiển thị bằng số tiền chính thức do Backend tính, không thể bị hack sửa đổi từ Inspect Element.
    * **Test Nút "Hủy giữ chỗ"**:
      * Bấm "Hủy giữ chỗ" -> FE gọi `DELETE /api/v1/bookings/:bookingId` -> Đơn chuyển `CANCELLED`, các ghế được mở khóa ngay lập tức trên sơ đồ ghế cho người khác đặt.
    * **Chuyển trạng thái đơn hàng**:
      * Bấm tiếp tục sang bước thanh toán: Trạng thái Booking tại backend chuyển thành `PENDING_PAYMENT`, thời gian còn lại của đồng hồ 3 phút vẫn được duy trì đồng bộ.

---

### Task BE-08: Bóc tách Payment Service & Webhook VNPay
* **Mục tiêu**: Tạo `services/payment-service` độc lập, quản lý giao dịch thanh toán và cổng VNPay.
* **Source Monolith**: `Payment`, `PaymentServiceImpl`, `VNPayServiceImpl`, `RefundServiceImpl`, `WalletServiceImpl`, `LoyaltyPointServiceImpl`.
* **Đích**: `services/payment-service/` (Port 8084).
* **Ràng buộc**:
  * `Payment` entity chỉ lưu `bookingId` hoặc `foodOrderId` dưới dạng Logical ID.
  * Endpoint tạo URL thanh toán: `POST /api/v1/payments/vnpay/create?bookingId=...`.
  * Endpoint xử lý VNPay IPN Webhook:
    * Kiểm tra chữ ký an toàn (`vnp_SecureHash`).
    * Kiểm tra số tiền `vnp_Amount` khớp với payment amount.
    * **Đảm bảo Idempotency**: Nếu VNPay gửi lại IPN lần 2, kiểm tra giao dịch đã `SUCCESS` thì không xử lý lại.
* **Nghiệm thu Backend**: Mock gọi VNPay IPN với chữ ký đúng ➔ `Payment` cập nhật trạng thái `SUCCESS` và lưu `paidAt`.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Khởi tạo thanh toán VNPay từ màn hình Checkout (`src/pages/user/BookingPage.jsx`)**:
    * Tại Bước 3, người dùng chọn phương thức thanh toán: "Cổng thanh toán điện tử VNPay (ATM / QR Pay / Visa)".
    * Bấm nút "Thanh toán với VNPay":
      * FE gọi `POST /api/v1/payments/vnpay/create?bookingId=${bookingId}` (thông qua `paymentService.createVnpayPayment`).
      * Backend tạo bản ghi `Payment` với trạng thái `PENDING` và trả về `data.paymentUrl` chứa chuỗi ký số VNPay.
      * Trình duyệt tự động chuyển hướng (Redirect) sang trang thanh toán chính thức của VNPay Sandbox (`https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...`).
  * **Màn hình Nhận kết quả thanh toán (`src/pages/user/PaymentCallbackPage.jsx` - Route `/payment-callback`)**:
    * Sau khi thao tác thanh toán hoặc bấm hủy trên trang VNPay, hệ thống tự động điều hướng về URL callback của FE: `/payment-callback?vnp_ResponseCode=...&vnp_TxnRef=...`.
    * **Kịch bản 1: Thanh toán Thành công (`vnp_ResponseCode === '00'`)**:
      * Giao diện hiển thị biểu tượng tick xanh lớn `CheckCircle`, dòng chữ: *"Thanh toán vé xem phim thành công!"*.
      * Chi tiết giao dịch hiển thị đầy đủ: Số tiền (`vnp_Amount / 100`), Mã đặt vé (`bookingCode`), Mã giao dịch ngân hàng (`vnp_TransactionNo`), Thời gian thanh toán.
      * Nút hành động "Xem vé của bạn" (`onContinue`): Click vào tự động dẫn tới trang `/tickets?highlightBookingId=...` để mở vé ngay lập tức.
    * **Kịch bản 2: Hủy giao dịch hoặc Thất bại (`vnp_ResponseCode !== '00'`)**:
      * Giao diện hiển thị biểu tượng cảnh báo màu đỏ `XCircle`, dòng chữ: *"Giao dịch bị hủy hoặc không thành công tại cổng VNPay"*.
      * Nút bấm cho phép quay lại đặt vé hoặc kiểm tra lại đơn hàng trong danh sách vé chờ thanh toán.

---

### Task BE-09: Tích hợp RabbitMQ & Transactional Outbox Pattern
* **Mục tiêu**: Kết nối bất đồng bộ giữa Payment Service và Booking Service khi thanh toán thành công.
* **Cơ chế**:
  1. **Tại Payment Service**:
     * Khi nhận IPN hợp lệ, trong cùng 1 Database Transaction: Lưu `PaymentStatus = SUCCESS` và ghi 1 bản ghi vào bảng `outbox_events` (Payload: `PaymentSucceededEvent` chứa `bookingId`, `paymentId`, `amount`).
     * Background Worker đọc `outbox_events` và publish lên RabbitMQ Exchange `cinema.payment.events` với Routing Key `payment.succeeded`.
  2. **Tại Booking Service**:
     * Lắng nghe Queue `booking.payment-succeeded.queue`.
     * Khi nhận event `PaymentSucceededEvent`:
       * Cập nhật `Booking.status = PAID`.
       * Cập nhật toàn bộ `BookingSeat.status = BOOKED`.
       * Sinh mã `qrCode` chứa thông tin check-in hợp lệ.
       * Lưu `eventId` vào bảng `processed_events` để chống xử lý lặp.
* **Nghiệm thu Backend**: Bắn event `PaymentSucceeded` vào RabbitMQ ➔ Booking tự động chuyển `PAID` và sinh mã QR mà không cần HTTP request trực tiếp giữa 2 service.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Trang Vé của tôi & Hiển thị Vé điện tử QR Code (`src/pages/user/MyTicketsPage.jsx` - Route `/tickets`)**:
    * **Cập nhật trạng thái tự động thành `PAID`**:
      * Sau khi thanh toán thành công, người dùng mở trang `/tickets`.
      * Booking tương ứng hiển thị Badge màu xanh lá: **`ĐÃ THANH TOÁN`** (thay cho `HOLDING` hoặc `PENDING_PAYMENT`).
    * **Kiểm tra hiển thị Mã QR Check-in**:
      * Thẻ vé vẽ mã QR sắc nét bằng `<QRCodeSVG value={ticket.qrCode} size={160} />`.
      * Phía dưới mã QR hiển thị chuỗi ký tự vé (ví dụ: `BK-2026-948123`) và hướng dẫn: *"Vui lòng xuất trình mã này tại quầy soát vé để vào rạp"*.
    * **Kích hoạt tính năng Đặt thêm Bắp nước (`canOrderMoreFood`)**:
      * Với các vé đã `PAID`, xuất hiện nút "Đặt thêm bắp nước" dẫn tới trang `/concessions?bookingId=...`, cho phép khách mua thêm đồ ăn bổ sung cho suất chiếu của mình.
    * **Kiểm tra chống thanh toán trễ (Expired Booking Safety)**:
      * Nếu VNPay callback về trễ khi vé đã hết hạn giữ chỗ (`EXPIRED`), hệ thống chuyển trạng thái `REFUNDED` / `CANCELLED`, FE hiển thị thông báo lỗi rõ ràng và không sinh mã QR check-in giả.

---

### Task BE-10: Cấu hình Spring Cloud API Gateway & Auth Forwarding
* **Mục tiêu**: Hoàn thiện `services/api-gateway` làm cổng vào duy nhất.
* **Đích**: `services/api-gateway/` (Port 8080).
* **Chức năng**:
  * Cấu hình Route định tuyến:
    * `/api/v1/auth/**`, `/api/v1/users/**` ➔ `identity-service:8081`
    * `/api/v1/movies/**`, `/api/v1/showtimes/**`, `/api/v1/cinemas/**`, `/api/v1/foods/**`, `/api/v1/ticket-pricing/**` ➔ `catalog-service:8082`
    * `/api/v1/bookings/**`, `/api/v1/staff/check-in/**` ➔ `booking-service:8083`
    * `/api/v1/payments/**`, `/api/v1/wallets/**`, `/api/v1/loyalty/**` ➔ `payment-service:8084`
    * `/api/v1/recommendation/**` ➔ `recommendation-service:8000`
  * Chặn các internal route (ví dụ `/internal/**` không cho public từ internet).
  * **JWT Validation Filter**: Verify chữ ký JWT token, trích xuất `userId` và `roles`, đính kèm vào Header `X-User-Id` và `X-User-Roles` để chuyển xuống các downstream service.
* **Nghiệm thu Backend**: Gửi request qua Gateway Port 8080 với Bearer Token, downstream service đọc được Header `X-User-Id` mà không cần gọi lại DB Identity.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Kiểm tra Hợp nhất Cổng gọi API trên toàn bộ Web**:
    * Mở DevTools (F12) -> Tab Network trên trình duyệt khi lướt web FE.
    * Tất cả request từ các trang khác nhau (`/`, `/movies`, `/tickets`, `/profile`, `/staff`, `/admin`) đều chỉ gửi tới một Domain & Port duy nhất: `http://localhost:8080/api/v1/...`.
    * Không còn bất kỳ request nào gọi thẳng đến port nội bộ `8081`, `8082`, `8083`, `8084`, `8000`.
  * **Kiểm tra Phân quyền Role trên Route FE**:
    * Đăng nhập tài khoản User thường: Thử gõ URL `/admin` hoặc `/staff` -> FE tự động chặn và redirect về trang chủ (`/`) hoặc trang báo không đủ quyền (`403`).
    * Đăng nhập tài khoản Staff: Menu tự động xuất hiện mục "Soát vé QR" (`/staff`).
    * Đăng nhập tài khoản Admin: Menu tự động xuất hiện mục "Quản trị hệ thống" (`/admin`).
  * **Kiểm tra Bảo mật Chặn Internal API từ Gateway**:
    * Mở Console hoặc cURL thử gọi: `http://localhost:8080/internal/v1/catalog/checkout-quote`.
    * Kết quả: Gateway lập tức chặn với mã lỗi `403 Forbidden` hoặc `404 Not Found`, chứng minh các API nội bộ giữa các microservice được cô lập an toàn tuyệt đối.

---

### Task BE-11: Tích hợp Recommendation Service (`AiService`) qua RabbitMQ
* **Mục tiêu**: Đấu nối dịch vụ AI gợi ý phim lắng nghe các sự kiện từ hệ thống để cập nhật ma trận tương tác.
* **Đích**: `services/recommendation-service/` (FastAPI).
* **Sự kiện tiêu thụ (Consumers)**:
  * Lắng nghe `booking.paid`: Ghi nhận tín hiệu tương tác mạnh (mua vé xem phim) cho cặp `(userId, movieId)`.
  * Lắng nghe `movie.published` / `movie.updated`: Cập nhật metadata, thể loại và vector embedding cho phim mới.
* **API cung cấp**: `GET /api/v1/recommendations/user/{userId}` (hoặc `/api/v1/recommendation/collaborative/{userId}`) và `GET /api/v1/recommendation/content/{movieId}`.
* **Nghiệm thu Backend**: Đặt vé xem phim X thành công ➔ Recommendation service tự động tăng điểm ưu tiên các phim cùng thể loại với X cho user đó.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Khu vực "Gợi ý cho bạn" trên Trang chủ (`src/pages/user/HomePage.jsx`)**:
    * Khi User đăng nhập, FE gọi `GET /api/v1/recommendation/collaborative/:userId` (thông qua `recommendationService.getCollaborativeRecommendations`).
    * Khu vực Carousel "Phim đề xuất riêng cho bạn" hiển thị danh sách các phim phù hợp với gu xem phim của người dùng, kèm lý do gợi ý (*"Dựa trên các phim bạn đã đặt vé gần đây"*).
  * **Khu vực "Phim tương tự" trên Trang Chi tiết phim (`src/pages/user/MovieDetailPage.jsx`)**:
    * Khi xem chi tiết 1 phim (ví dụ: phim kinh dị A), FE gọi `GET /api/v1/recommendation/content/:movieId`.
    * Dưới chân trang hiển thị danh sách 4-6 phim tương tự (Content-based filtering) dựa trên tương đồng về thể loại, đạo diễn, dàn diễn viên.
  * **Test Cập nhật Gợi ý Thời gian thực (Real-time Adaptive)**:
    1. Tài khoản User B chưa từng mua vé thể loại Anime -> Danh sách gợi ý chủ yếu là phim Hành động.
    2. User B tiến hành đặt vé và thanh toán thành công 1 phim Anime (ví dụ: *"Doraemon"*).
    3. Sự kiện `booking.paid` bắn qua RabbitMQ -> AI Service tính toán cập nhật ma trận người dùng.
    4. Quay lại Trang chủ F5: Danh mục "Gợi ý cho bạn" lập tức xuất hiện các phim Anime và Hoạt hình liên quan ở vị trí ưu tiên cao nhất.

---

### Task BE-12: End-to-End Integration & Docker Compose Validation
* **Mục tiêu**: Đóng gói toàn bộ hệ thống và kiểm thử tích hợp toàn diện từ máy sạch.
* **Yêu cầu**:
  1. Viết `Dockerfile` tối ưu (Multi-stage build) cho từng service.
  2. Hoàn thiện `infra/docker-compose.yml` liên kết toàn bộ:
     * 5 PostgreSQL DBs.
     * 1 RabbitMQ Cluster.
     * 5 Spring Boot Services + 1 FastAPI AI Service + 1 Web Frontend.
  3. Viết 1 script test tự động (`test-e2e.sh` hoặc Postman Collection Runner) chạy xuyên suốt:
     `Register ➔ Login ➔ Get Movie ➔ Hold Seat ➔ Checkout ➔ Mock Payment ➔ Verify Booking PAID & QR ➔ Get Recommendation`.
* **Nghiệm thu Backend**: Chạy lệnh `docker compose up --build` từ một môi trường mới, toàn bộ hệ thống khởi động xanh và pass 100% kịch bản kiểm thử tích hợp.
* **Màn hình & Chức năng Frontend (FE) test được**:
  * **Kiểm thử Toàn diện Luồng Khách Hàng (Customer E2E Journey)**:
    * Đi từ đầu đến cuối trên trình duyệt:
      1. Khách mở `http://localhost:5173` -> Đăng ký tài khoản -> Đăng nhập.
      2. Trang chủ: Chọn phim đang chiếu -> Xem chi tiết lịch chiếu -> Chọn khung giờ 19:30.
      3. Sơ đồ ghế: Giữ ghế `E05, E06` -> Countdown 3:00 xuất hiện.
      4. Chọn vé & Bắp nước: Chọn 2 vé Người lớn + 1 Combo bắp nước.
      5. Trang Checkout: Kiểm tra tóm tắt đơn giá từ Catalog Quote -> Chọn VNPay -> Bấm thanh toán.
      6. Cổng Sandbox VNPay: Nhập thẻ test sandbox -> Bấm Xác nhận OTP.
      7. Trang Callback: Nhận thông báo "Thanh toán thành công" -> Bấm "Xem vé của bạn".
      8. Trang Vé: Mã QR check-in hiển thị hợp lệ, trạng thái `ĐÃ THANH TOÁN`.
  * **Kiểm thử Màn hình Nhân viên Soát vé (`src/pages/staff/StaffCheckInPage.jsx` - Route `/staff`)**:
    * Đăng nhập tài khoản Nhân viên (`ROLE_STAFF`).
    * Mở camera quét mã QR trên điện thoại của khách hàng (hoặc nhập mã `bookingCode`).
    * FE gọi `GET /api/v1/staff/check-in/lookup?bookingCode=...`: Hiển thị chi tiết vé, ghế `E05, E06`, tên phim, phòng chiếu.
    * Bấm nút **"Xác nhận Check-in"**: FE gọi `POST /api/v1/staff/check-in` -> Vé đổi trạng thái sang `USED` (Đã sử dụng).
    * **Test Chống gian lận (Double Check-in Prevention)**: Quét lại chính mã QR đó lần thứ 2 -> Giao diện hiện cảnh báo đỏ: *"Cảnh báo: Vé này đã được check-in vào lúc HH:mm bởi nhân viên khác!"*.
  * **Kiểm thử Màn hình Quản trị viên Toàn năng (`src/pages/admin/AdminPage.jsx` - Route `/admin/*`)**:
    * Đăng nhập tài khoản Admin (`ROLE_ADMIN`).
    * **Quản lý Phim (`AdminMoviesPanel`)**: Thêm phim mới, upload poster ảnh, sửa trạng thái từ `UPCOMING` sang `NOW_SHOWING`.
    * **Quản lý Suất chiếu (`AdminShowtimesPanel`)**: Lên lịch chiếu mới cho phòng `Hall 1` vào ngày mai.
    * **Quản lý Bắp nước (`AdminFoodsPanel`)**: Chỉnh sửa tồn kho món ăn, tắt mở trạng thái món `OUT_OF_STOCK`.
    * **Báo cáo Thống kê Doanh thu (`AdminOverviewPanel` & `AdminStatsPanel`)**: Xem biểu đồ doanh thu theo ngày, số lượng vé đã bán ra được cập nhật số liệu chính xác theo các đơn hàng vừa thanh toán.

---

## 5. Bảng ma trận đối soát kiểm thử Frontend (Backend Task ➔ Màn hình & Kịch bản FE)

Bảng tổng hợp dưới đây giúp Leader và Tester dễ dàng đối chiếu tiến độ giữa công việc Backend và chức năng trực quan trên Frontend:

| Backend Task | Màn hình Frontend tương ứng (`FE/cinepremier`) | Route FE | Chức năng & Kịch bản người dùng kiểm thử được trên FE |
|---|---|---|---|
| **BE-01: Scaffolding & Hạ tầng** | App Shell (`AppRoutes.jsx`, `UserLayout.jsx`) | `/` | Chạy dev server Vite, FE kết nối tới Gateway `localhost:8080/actuator/health` không có lỗi mạng hay CORS. |
| **BE-02: Identity Service** | Modal Xác thực (`AuthModal.jsx`) & Hồ sơ (`ProfilePage.jsx`) | `/`, `/profile` | Đăng ký tài khoản, xác thực OTP email, Đăng nhập nhận JWT token, Hiển thị avatar/tên trên Header, Đăng xuất, Đổi mật khẩu cá nhân. |
| **BE-03: Catalog Core** | Trang chủ (`HomePage.jsx`), Khám phá (`ExplorePage.jsx`), Chi tiết phim (`MovieDetailPage.jsx`), Bắp nước (`ConcessionsPage.jsx`) | `/`, `/movies`, `/movies/:id`, `/concessions` | Lướt xem phim đang chiếu / sắp chiếu, Lọc phim theo thể loại, Xem chi tiết phim kèm trailer YouTube và diễn viên, Xem danh mục combo bắp nước kèm giá. |
| **BE-04: Catalog Quote API** | Đặt vé - Bước 2 Chọn vé & F&B (`BookingPage.jsx`) | `/movies/:id/book` | Chọn số lượng vé Người lớn/Sinh viên/Trẻ em, Chọn bắp nước, Backend tự tính phụ thu ghế VIP / ngày lễ và trả giá thẩm quyền. |
| **BE-05: Booking Core & Snapshot** | Vé của tôi (`MyTicketsPage.jsx`), Lịch sử đơn bắp nước (`FoodOrdersHistoryPage.jsx`) | `/tickets` | Xem danh sách vé đã mua với dữ liệu Snapshot bất biến (tên phim, phòng chiếu, đơn giá lúc mua không bị đổi khi Catalog thay đổi). |
| **BE-06: Seat Hold Concurrency** | Sơ đồ ghế tương tác (`BookingPage.jsx` - Bước 1) | `/movies/:id/book` | Chọn ghế trống trên Seat Map, Đồng hồ đếm ngược 3 phút kích hoạt, Mở 2 tab tranh ghế nhận lỗi `409 Conflict`, Hết 3 phút ghế tự động nhả về trạng thái trống. |
| **BE-07: Checkout & Snapshot** | Xác nhận đơn hàng (`BookingPage.jsx` - Bước 3) | `/movies/:id/book` | Hiển thị bảng kê chi tiết ghế và bắp nước kèm tổng tiền server tính, Nút hủy giữ chỗ giải phóng ghế ngay lập tức, chuyển trạng thái `PENDING_PAYMENT`. |
| **BE-08: Payment & VNPay** | Thanh toán (`BookingPage.jsx`) & Kết quả thanh toán (`PaymentCallbackPage.jsx`) | `/movies/:id/book`, `/payment-callback` | Tự động chuyển hướng sang cổng VNPay Sandbox, Xử lý callback mã 00 hiển thị màn hình thanh toán thành công kèm mã giao dịch ngân hàng. |
| **BE-09: RabbitMQ & Outbox** | Chi tiết vé xem phim (`MyTicketsPage.jsx`) | `/tickets` | Vé tự động chuyển trạng thái `ĐÃ THANH TOÁN` (`PAID`), Hiển thị mã QR SVG check-in sắc nét, mở khóa nút mua thêm bắp nước. |
| **BE-10: API Gateway & Auth** | Toàn bộ các trang Web | `/*` | Toàn bộ request FE trỏ qua một Port 8080 duy nhất, Header `X-User-Id` tự động inject, Đường dẫn nội bộ `/internal/**` bị chặn 403 an toàn. |
| **BE-11: Recommendation Service** | Gợi ý cho bạn (`HomePage.jsx`) & Phim tương tự (`MovieDetailPage.jsx`) | `/`, `/movies/:id` | Hiển thị Carousel phim gợi ý riêng theo sở thích người dùng, Sau khi mua vé thể loại nào thì trang chủ ưu tiên đề xuất thể loại đó theo thời gian thực. |
| **BE-12: E2E & Admin/Staff** | Soát vé nhân viên (`StaffCheckInPage.jsx`) & Quản trị (`AdminPage.jsx`) | `/staff`, `/admin/*` | Staff quét camera mã QR check-in vé thành công (chặn check-in lặp lần 2), Admin quản lý phim, mở suất chiếu, quản lý phòng/ghế và xem biểu đồ doanh thu. |

---

## 6. Kế hoạch chi tiết 8 tuần cho nhóm 6 người

```text
Tuần 1 (16/09–22/09): Khóa Scope, Contract & Dựng Khung Mono-repo
Tuần 2 (23/09–29/09): Tách Identity, Catalog & Core Read APIs
Tuần 3 (30/09–06/10): Tách Booking, Concurrency Giữ Ghế & Checkout Quote
Tuần 4 (07/10–13/10): Tách Payment, Tích hợp VNPay, RabbitMQ & QR Ticket
Tuần 5 (14/10–20/10): Tích hợp Recommendation Event & Màn hình Web/Flutter
Tuần 6 (21/10–27/10): Hoàn thiện Độ tin cậy (Outbox, Idempotency) & Feature Freeze
Tuần 7 (28/10–03/11): Load Testing, Sửa lỗi Concurrency & Release Candidate
Tuần 8 (04/11–11/11): Đóng băng Code, Hoàn thiện Paper & Diễn tập Demo
```

### Tuần 1 — 16/09 đến 22/09: Khóa Scope, Contract & Dựng Khung Mono-repo
* **Leader**: Chốt service boundary, tạo cấu trúc thư mục Mono-repo, viết `docker-compose.yml` khởi tạo 5 DB và RabbitMQ (**Task BE-01**).
* **Người 2**: Liệt kê chi tiết Entity Identity/Catalog, chốt OpenAPI cho Auth, Movie, Showtime, Pricing.
* **Người 3**: Liệt kê code Booking/Payment, chốt hợp đồng 7 Logical IDs và thiết kế Quote Snapshot payload.
* **Người 4 (Web)**: Khởi tạo project Web, chuẩn hóa Design Tokens, component thư viện (Buttons, Inputs, Modals).
* **Người 5 (Flutter)**: Khởi tạo dự án Flutter (Riverpod + GoRouter + Dio), chuyển design system sang Flutter Theme.
* **Người 6 (AI)**: Chuẩn hóa dataset phim, pipeline làm sạch dữ liệu và interaction schema (view, wishlist, booking).
* **Mốc kiểm tra tuần 1**: Cả nhóm chạy được `docker compose` local gồm các DB và RabbitMQ; Web/Flutter gọi được mock API đúng OpenAPI.

### Tuần 2 — 23/09 đến 29/09: Tách Identity, Catalog & Core Read APIs
* **Leader**: Dựng khung `api-gateway`, cấu hình reverse proxy cơ bản, log `X-Correlation-Id`.
* **Người 2**: Thực hiện **Task BE-02** (tách Identity Service) và **Task BE-03** (tách Catalog Service read APIs).
* **Người 3**: Thực hiện **Task BE-05** (dựng skeleton Booking Service với các trường Snapshot).
* **Người 4 (Web)**: Tích hợp API thật cho Login/Register, xây dựng trang Home, Movie List, Movie Detail.
* **Người 5 (Flutter)**: Màn hình Login/Register, Home, Movie Detail và chọn ngày/suất chiếu trên điện thoại.
* **Người 6 (AI)**: Dựng baseline Content-Based Recommendation từ metadata phim (Genre, Actor, Director).
* **Mốc kiểm tra tuần 2**: Đăng nhập qua Gateway lấy JWT, xem danh sách phim và lịch chiếu bằng API thật.

### Tuần 3 — 30/09 đến 06/10: Giữ ghế Concurrency & Checkout Quote
* **Leader**: Hỗ trợ tích hợp Gateway route cho Booking, kiểm tra timeout kết nối giữa các service.
* **Người 2**: Thực hiện **Task BE-04** (hoàn thành API `POST /internal/v1/catalog/checkout-quote`).
* **Người 3**: Thực hiện **Task BE-06** (giữ ghế 3 phút an toàn, xử lý xung đột 2 người giữ 1 ghế) và **Task BE-07** (gọi quote API và lưu snapshot).
* **Người 4 (Web)**: Xây dựng Seat Map trực quan, hiển thị countdown 3 phút, xử lý khi ghế bị người khác giữ.
* **Người 5 (Flutter)**: Xây dựng Seat Map mobile mượt mà, bộ đếm ngược 3 phút, màn hình chọn combo bắp nước.
* **Người 6 (AI)**: Cài đặt thuật toán Collaborative Filtering (BPR / Matrix Factorization) trên dữ liệu tương tác mẫu.
* **Mốc kiểm tra tuần 3**: Giữ ghế 3 phút thành công, tính đúng tổng tiền vé kèm bắp nước tại backend, lưu snapshot bất biến.

### Tuần 4 — 07/10 đến 13/10: Thanh toán VNPay, RabbitMQ & QR End-to-End
* **Leader**: Cấu hình RabbitMQ Exchange/Queue chuẩn, hỗ trợ cấu hình mạng cho VNPay callback.
* **Người 2**: Phát các event từ Catalog khi có phim mới hoặc suất chiếu bị hủy.
* **Người 3**: Thực hiện **Task BE-08** (tách Payment Service, VNPay webhook) và **Task BE-09** (kết nối RabbitMQ cập nhật `PAID`, sinh mã QR).
* **Người 4 (Web)**: Màn hình chuyển hướng thanh toán, kết quả Payment Success/Failed, trang chi tiết vé kèm QR.
* **Người 5 (Flutter)**: Màn hình kết quả thanh toán, trang Vé của tôi (My Tickets) hiển thị mã QR check-in.
* **Người 6 (AI)**: Lắng nghe event `BookingPaid` để cập nhật ma trận người dùng theo thời gian thực.
* **Mốc kiểm tra tuần 4 (QUAN TRỌNG NHẤT)**: Toàn bộ luồng từ Đăng nhập ➔ Chọn phim ➔ Giữ ghế ➔ Checkout ➔ Thanh toán VNPay ➔ Nhận QR ticket chạy thông suốt trên cả Web và Flutter.

### Tuần 5 — 14/10 đến 20/10: Recommendation & Quản trị Admin
* **Leader**: Hoàn thiện bảo mật Gateway, phân quyền Role (`ADMIN`, `STAFF`, `CUSTOMER`) qua Header.
* **Người 2**: Hoàn thiện các API Admin CRUD Phim, Suất chiếu, Phòng chiếu và Bắp nước.
* **Người 3**: Hoàn thiện tính năng Quét mã QR Check-in cho nhân viên và luồng Hoàn tiền (Refund) khi hủy suất chiếu.
* **Người 4 (Web)**: Màn hình Admin quản lý suất chiếu/phim, mục gợi ý phim cá nhân hóa trên trang chủ.
* **Người 5 (Flutter)**: Tích hợp mục "Phim đề xuất cho bạn", tính năng Wishlist và Review phim.
* **Người 6 (AI)**: Thực hiện **Task BE-11** (kết hợp Adaptive Hybrid: Content-based + Collaborative Filtering), tính toán metrics (Precision@K, NDCG@K).
* **Mốc kiểm tra tuần 5**: Quét mã QR check-in thành công; mục gợi ý phim hiển thị đúng danh sách từ AI Service.

### Tuần 6 — 21/10 đến 27/10: Hoàn thiện Tin cậy & Feature Freeze
* **Leader**: Rà soát triệt để Database Isolation (không service nào gọi lén DB service khác), hoàn thiện Outbox pattern.
* **Người 2**: Kiểm tra tính ổn định dữ liệu Catalog, bổ sung validation và index cơ sở dữ liệu.
* **Người 3**: Kiểm tra Idempotency xử lý callback VNPay lặp và race condition khi thanh toán sát giờ hết hạn giữ ghế.
* **Người 4 (Web)**: Tối ưu UI/UX, kiểm tra toàn bộ trạng thái Loading, Empty, Error trên mọi kích thước màn hình.
* **Người 5 (Flutter)**: Xử lý mất mạng đột ngột, token hết hạn tự động refresh, tối ưu hiệu năng app.
* **Người 6 (AI)**: Chốt bảng kết quả thực nghiệm mô hình, viết phần Methodology và Results cho Paper.
* **Mốc kiểm tra tuần 6 (27/10)**: **ĐÓNG BĂNG TÍNH NĂNG (Feature Freeze)**. Không thêm feature mới, chuyển sang ổn định hệ thống.

### Tuần 7 — 28/10 đến 03/11: Load Testing & Release Candidate
* **Leader**: Thực hiện **Task BE-12** (đóng gói Docker Compose toàn diện, script test tự động từ máy sạch).
* **Người 2 & 3**: Chạy Load Test (JMeter/K6) mô phỏng 500 người dùng đồng thời tranh giành ghế trong 1 suất chiếu hot; sửa triệt để các lỗi Deadlock hoặc dữ liệu không nhất quán.
* **Người 4 (Web)**: Đóng gói bản build Production Web, test trên Chrome, Safari, Edge.
* **Người 5 (Flutter)**: Xuất file APK Release Candidate, test trên các thiết bị Android vật lý khác nhau.
* **Người 6 (AI)**: Hoàn thành bản thảo Paper hoàn chỉnh (Abstract, Intro, Related Work, Method, Experiments, Conclusion).
* **Mốc kiểm tra tuần 7 (03/11)**: Có bản **Release Candidate (RC)** chạy ổn định từ 1 dòng lệnh `docker compose up`.

### Tuần 8 — 04/11 đến 11/11: Đóng băng & Diễn tập Bàn giao
* **Cả nhóm**: 
  * Chạy diễn tập Demo (Rehearsal) theo đúng kịch bản chấm điểm của hội đồng.
  * Chuẩn bị sẵn bộ Seed Data mẫu hoàn hảo (phim đang chiếu, suất chiếu hôm nay, tài khoản admin/staff/customer).
  * **08/11**: Đóng băng toàn bộ Code, Schema và Tài liệu.
  * **11/11**: Bàn giao đồ án thành công.

---

## 7. Definition of Done (DoD) cho từng Task

Bất kỳ task nào khi giao cho AI hoặc thành viên thực hiện chỉ được đánh dấu **DONE** khi đáp ứng đủ 8 tiêu chí:
1. **Không vi phạm Database Boundary**: Không import JPA Entity hoặc Repository của service khác.
2. **Không tin tưởng Client**: Mọi phép tính tiền đều do backend thực hiện dựa trên Quote của Catalog.
3. **Có Snapshot bảo toàn**: Dữ liệu hóa đơn/vé trong quá khứ không bị ảnh hưởng khi Catalog đổi giá.
4. **Xử lý Idempotent**: Lệnh thanh toán hoặc event xử lý 2 lần không tạo ra 2 vé trùng.
5. **Có migration script sạch**: Database khởi tạo thành công qua script Flyway độc lập.
6. **Có Healthcheck**: Service cung cấp endpoint `/actuator/health` hoạt động chính xác.
7. **Chạy được trong Docker Compose**: Không phụ thuộc vào đường dẫn tuyệt đối của máy cá nhân.
8. **Kiểm thử được trên Frontend**: Tính năng đã được kiểm chứng hoạt động trực quan trên màn hình FE tương ứng theo Bảng ma trận đối soát (Mục 5).
