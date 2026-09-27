# HƯỚNG DẪN REVIEW KỸ THUẬT & BÀN GIAO TASK (TECHNICAL REVIEW GUIDE)
**Dự án**: CinemaAI Platform (MSS301)  
**Phân hệ phụ trách**: Catalog Core & Master Data  
**Người thực hiện**: Vũ (Backend 1)  
**Mã Task bàn giao**: `BE-02`, `BE-03`, `BE-04` & `Admin CRUD`

---

## 1. TỔNG QUAN PHẠM VI TASK BÀN GIAO

Tài liệu này dùng để các thành viên trong nhóm (**Leader/DevOps**, **Booking Backend**, **Mobile Lead/Dev**) thực hiện kiểm thử nghiệm thu (Code Review & Functional Testing) các chức năng đã hoàn thành:

| Mã Task | Tên nhiệm vụ | Service đích | Cổng (Port) | Database riêng |
|---|---|---|:---:|:---:|
| **BE-02** | Identity & Access Service (Auth, JWT, User Profile) | `identity-service` | `8081` | `identity_db` (PostgreSQL) |
| **BE-03** | Catalog Core Service (Phim, Suất chiếu, Phòng/Ghế, Bắp nước, Giá vé) | `catalog-service` | `8082` | `catalog_db` (PostgreSQL) |
| **BE-04** | Authoritative Checkout Quote API (`checkout-quote`) | `catalog-service` | `8082` | `catalog_db` |
| **Admin CRUD** | Quản lý Phim, Suất chiếu (đơn & bulk) và Bắp nước cho Quản trị viên | `catalog-service` | `8082` | `catalog_db` |

---

## 2. CHUẨN BỊ MÔI TRƯỜNG REVIEW (PREREQUISITES)

### 2.1. Yêu cầu môi trường
* **Java**: **JDK 21** *(Lưu ý: Bắt buộc dùng JDK 21 để tương thích với Spring Boot 3 và Lombok, không dùng JDK 22+).*
* **Node.js**: v18+ & npm.
* **Docker & Docker Compose**: Để chạy 5 CSDL PostgreSQL độc lập và RabbitMQ.

### 2.2. Khởi động Hạ tầng Docker (PostgreSQL & RabbitMQ)
Tại thư mục `MSS301-Backend-dev`:
```powershell
docker compose up -d
```
> Kiểm tra bằng lệnh `docker ps`, đảm bảo `cinema-identity-db` (cổng 5431) và `cinema-catalog-db` (cổng 5432) đang ở trạng thái `healthy`.

### 2.3. Khởi động các Microservices Backend
Mở 3 cửa sổ Terminal riêng biệt:

* **Terminal 1: API Gateway (Cổng 8080)**
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.10"
  $env:Path="C:\Program Files\Java\jdk-21.0.10\bin;$env:Path"
  cd cinema-services/api-gateway
  mvn spring-boot:run
  ```
* **Terminal 2: Identity Service (Cổng 8081)**
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.10"
  $env:Path="C:\Program Files\Java\jdk-21.0.10\bin;$env:Path"
  cd cinema-services/identity-service
  mvn spring-boot:run
  ```
* **Terminal 3: Catalog Service (Cổng 8082)**
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.10"
  $env:Path="C:\Program Files\Java\jdk-21.0.10\bin;$env:Path"
  cd cinema-services/catalog-service
  mvn spring-boot:run
  ```

### 2.4. Khởi động Web Frontend
* **Terminal 4: Frontend (Cổng 5173)**
  ```powershell
  cd MSS301-Frontend-main
  npm run dev
  ```
  Truy cập: `http://localhost:5173`

---

## 3. THÔNG TIN TÀI KHOẢN TEST SẴN CÓ (SEED DATA)

| Vai trò | Email đăng nhập | Mật khẩu | Quyền hạn |
|---|---|---|---|
| **ADMIN** | `admin@cinemaai.com` | `Admin123` | Quản trị toàn hệ thống, CRUD Catalog, Users |
| **STAFF** | `staff@cinemaai.com` | `Staff123` | Nhân viên rạp (soát vé, hỗ trợ) |
| **CUSTOMER** | Tự đăng ký trực tiếp trên Web | Tùy chọn | Khách hàng mua vé, xem lịch chiếu |

---

## 4. CHECKLIST REVIEW CHI TIẾT THEO TỪNG TASK

### ✅ Task BE-02: Identity Service
1. **Kiểm tra Unit Test**:
   ```powershell
   mvn test -pl identity-service
   ```
   *Kết quả kỳ vọng*: `BUILD SUCCESS`, `JwtServiceTest` pass 100%.
2. **Kiểm tra trên Web (`http://localhost:5173`)**:
   - [x] **Đăng nhập**: Bấm Đăng nhập -> dùng `admin@cinemaai.com` / `Admin123` -> Đăng nhập thành công, xuất hiện menu "Quản trị".
   - [x] **Xem & Sửa Profile**: Vào `/profile` -> Xem họ tên, đổi số điện thoại -> Bấm Lưu -> Dữ liệu cập nhật thành công (`PUT /api/v1/users/me`).
   - [x] **Đổi mật khẩu**: Thử nhập mật khẩu cũ và mới -> Thông báo thành công (`POST /api/v1/users/me/password`).
   - [x] **Đăng ký tài khoản mới**: Mở modal Đăng ký -> Điền thông tin -> Nhận mã OTP qua email và kích hoạt tài khoản.
   - [x] **Admin quản lý User**: Vào `/admin/system/users` -> Xem danh sách user, thử tạo tài khoản Staff mới (`POST /api/v1/admin/users/staff`).

---

### ✅ Task BE-03: Catalog Core Service
1. **Kiểm tra Unit Test & Flyway Migration**:
   ```powershell
   mvn test -pl catalog-service
   ```
   *Kết quả kỳ vọng*: `BUILD SUCCESS`, Flyway migration schema `V1__init_catalog.sql` pass.
2. **Kiểm tra trên Web**:
   - [x] **Trang chủ (`/`)**: Hiển thị Banner phim demo ("CinemaAI Demo"), danh sách phim đang chiếu (`NOW_SHOWING`).
   - [x] **Trang Khám phá (`/movies`)**: Lọc phim theo thể loại (Adventure, Action,...), thanh tìm kiếm từ khóa hoạt động thời gian thực.
   - [x] **Chi tiết phim (`/movies/1`)**: Hiển thị diễn viên, thời lượng, nhãn tuổi P, cốt truyện và danh sách ngày chiếu có suất.
   - [x] **Trang Lịch chiếu (`/showtimes`)**: Xem lưới lịch chiếu theo rạp và phòng chiếu (Hall 1).
   - [x] **Trang Bắp nước (`/concessions`)**: Hiển thị danh mục món lẻ và combo bắp nước kèm giá niêm yết từ Catalog.

---

### ✅ Task BE-04: Authoritative Quote API (`checkout-quote`)
API then chốt chịu trách nhiệm tính giá có thẩm quyền từ Backend, không phụ thuộc vào giá tính ở Client.

* **Endpoint kiểm tra**:
  * Nội bộ (Inter-service): `POST http://localhost:8080/internal/v1/catalog/checkout-quote`
  * Frontend Facade: `POST http://localhost:8080/api/v1/catalog/checkout-quote`
* **Kịch bản kiểm thử trên Web (`/booking`)**:
  1. Chọn suất chiếu hôm nay lúc 18:00 (Hall 1).
  2. **Chọn ghế**: Chọn ghế `A01` -> Sang Bước 2: Chọn loại vé và thêm bắp nước.
  3. **Quan sát tổng tiền**: Frontend tự động gọi API `checkout-quote` ngầm:
     - Giá từng ghế và vé phản ánh đúng phụ thu và loại vé.
     - Tiền bắp nước cộng dồn chính xác.
     - Trả về `validUntil` có hạn 5 phút (TTL 300s).
* **Kiểm tra các quy tắc an toàn (Edge cases)**:
  - [x] Số lượng vé lệch với số ghế đã chọn -> Backend từ chối với lỗi 400 rõ ràng.
  - [x] Ghế thuộc phòng khác -> Backend từ chối hợp lệ.
  - [x] Suất chiếu cuối tuần / đêm muộn -> Tự động tính phụ thu chính xác.

---

### ✅ Admin CRUD: Phim, Suất chiếu, Bắp nước
Truy cập giao diện Quản trị tại `http://localhost:5173/admin`:

1. **Quản lý Phim (`AdminMoviesPanel`)**:
   - [x] Thêm phim mới: Bấm `+ Thêm phim mới`, nhập thông tin (thời lượng 60-180 phút, trailer, poster) -> Lưu thành công (`POST /api/v1/admin/movies`).
   - [x] Chỉnh sửa & Đổi trạng thái: Chuyển trạng thái giữa `NOW_SHOWING` / `UPCOMING` / `ENDED`.
   - [x] Xóa phim: Đổi trạng thái sang `INACTIVE` (`DELETE /api/v1/admin/movies/{id}`).
2. **Quản lý Suất chiếu (`AdminShowtimesPanel`)**:
   - [x] Tạo suất chiếu đơn: Chọn phim, phòng chiếu, giờ chiếu -> Kiểm tra chống trùng giờ chiếu (`POST /api/v1/admin/showtimes`).
   - [x] Tạo suất chiếu theo lô (`Bulk`): Tạo nhiều khung giờ cùng lúc trong 1 transaction (`POST /api/v1/admin/showtimes/bulk`).
   - [x] Mở bán suất chiếu: Chuyển trạng thái từ `SCHEDULED` sang `OPEN` để khách thấy trên web.
   - [x] Hủy suất chiếu: Thực hiện hủy suất chiếu kèm lý do sự cố.
3. **Quản lý Bắp nước (`AdminFoodsPanel`)**:
   - [x] CRUD món lẻ (`/items`) và combo (`/combos`): Thêm món mới, sửa giá bán, bật/tắt trạng thái hết hàng (`OUT_OF_STOCK`), xóa món.

---

## 5. HỢP ĐỒNG KẾT NỐI (CONTRACT) CHO CÁC THÀNH VIÊN TIẾP THEO

### 5.1. Dành cho Backend 2 (Khang - Booking Service `BE-05 / BE-06 / BE-07`)
Khi bạn thực hiện tính năng Giữ ghế và Tạo đơn đặt vé:
* **Endpoint gọi sang Catalog**: `POST /internal/v1/catalog/checkout-quote`
* **Header bắt buộc**: `X-Gateway-Secret: ${INTERNAL_GATEWAY_SECRET}`
* **Payload gửi sang**:
  ```json
  {
    "showtimeId": 1,
    "seatIds": [1, 2],
    "tickets": [
      { "seatId": 1, "ticketType": "ADULT", "viewerAge": 22, "quantity": 1 },
      { "seatId": 2, "ticketType": "STUDENT", "viewerAge": 20, "quantity": 1 }
    ],
    "foods": [
      { "productId": 1, "isCombo": false, "quantity": 1 }
    ]
  }
  ```
* **Payload Catalog trả về (Immutable Snapshot)**:
  Booking Service dùng dữ liệu này để lưu snapshot bất biến (`movieTitle`, `roomName`, `unitPrice`, `subtotal`), không lưu giá client gửi lên.

### 5.2. Dành cho Mobile Team (Quyết & Vy)
* Toàn bộ API Client đều đi qua Single Entry Point API Gateway tại `http://<host-ip>:8080/api/v1/...`.
* Token JWT gửi qua Header: `Authorization: Bearer <accessToken>`.

---

## 6. GHI CHÚ VỀ RANH GIỚI HỆ THỐNG (SCOPE BOUNDARY)
* **Tính năng Giữ ghế 3 phút (`/api/v1/bookings/hold`)** và **Thanh toán VNPay (`/api/v1/payments/...`)** thuộc về **Task BE-05/06/08**. Khi test web hiện tại, luồng đặt vé dừng tại bước hiển thị giá Quote thành công (các bước sau sẽ báo lỗi do `booking-service` chưa khởi chạy).
