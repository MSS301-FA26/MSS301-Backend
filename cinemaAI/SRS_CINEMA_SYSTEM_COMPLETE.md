# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)

## Hệ thống đặt vé xem phim trực tuyến CinemaAI / CinePremier

> **Phiên bản tài liệu:** 2.0  
> **Ngày đối chiếu mã nguồn:** 15/06/2026  
> **Phạm vi:** Backend Spring Boot, frontend React và các tích hợp liên quan  
> **Nguồn tổng hợp:** `SRS_CINEMA_SYSTEM.md`, `phase_mapping_srs_vs_docx.md` và mã nguồn hiện tại

---

## 0. Cập nhật 25/06/2026 (đối chiếu lại mã nguồn BE = `be/new`)

> Các điểm sau **ghi đè** trạng thái cũ bên dưới (bản 15/06 đã lỗi thời ở những mục này):

**Đã hoàn thành thêm (bản cũ ghi CHƯA):**
- **REV-01→05 Review:** đã có `ReviewController` + `AdminReviewController` + `ReviewServiceImpl` (xem/tạo/sửa/xóa, average-rating, ẩn/xóa). → **ĐÃ HOÀN THÀNH (BE)**.
- **RPT-01→03 Báo cáo:** đã có `AdminReportController` (`/revenue`, `/top-movies`, `/occupancy`) + `ReportService`. → **ĐÃ HOÀN THÀNH (BE)**.
- **LOY-05 Đổi điểm:** đã có `POST /api/v1/loyalty/me/redeem`. → **ĐÃ HOÀN THÀNH (BE)**.
- **SHOW-08 Scheduler suất chiếu:** đã có `ShowtimeStatusScheduler`. → **ĐÃ HOÀN THÀNH**.
- **NOTI-04 Đánh dấu tất cả đã đọc:** đã có `PATCH /api/v1/notifications/me/read-all`. → **ĐÃ HOÀN THÀNH (BE)**.
- **NFR-SEC-03 Secret:** đã externalize qua biến môi trường `${ENV}` + `.env`. → **ĐÃ HOÀN THÀNH**.

**Đã loại bỏ / sai khác so với bản cũ:**
- **Wallet bị bỏ hoàn toàn.** Không còn `Wallet`/`WalletService`/`WalletController`. Mọi yêu cầu "hoàn tiền vào wallet" (PAY-09, SHOW-07, LOY-03, Phase 6) **không áp dụng**. Khi ADMIN hủy suất: booking → `REFUNDED` + thu hồi loyalty + notification, **không cộng ví**.
- **Database chính thức vẫn là PostgreSQL.** `application.properties` đang trỏ MySQL chỉ vì là cấu hình chạy local trên máy dev, không phải thay đổi target — DB-03/DB-05/P0#1 giữ nguyên theo PostgreSQL.

**Còn thiếu thật (bản cũ vẫn đúng):** ký QR (TICKET-02), DB lock chống double-booking (BOOK-04), Flyway/migration chuẩn, email vé QR, FE test.

**Cần quyết:** `POST /api/v1/bookings/{id}/refund-request` (customer refund) **vẫn còn trong code** dù PAY-07 yêu cầu bỏ.

---

## 1. Mục đích tài liệu

Tài liệu này mô tả yêu cầu phần mềm của hệ thống CinemaAI / CinePremier và đối chiếu trực tiếp với trạng thái triển khai hiện tại.

Tài liệu sử dụng ba trạng thái:

| Trạng thái | Ý nghĩa |
|---|---|
| **ĐÃ HOÀN THÀNH** | Có luồng nghiệp vụ chính, API hoặc giao diện tương ứng và có thể sử dụng trong phạm vi đã mô tả. |
| **HOÀN THÀNH MỘT PHẦN** | Đã có một phần BE, FE, entity hoặc UI nhưng chưa hoàn chỉnh end-to-end, còn dùng mock hoặc thiếu điều kiện nghiệp vụ quan trọng. |
| **CHƯA THỰC HIỆN** | Chưa có controller/service/UI đáp ứng yêu cầu. Entity hoặc repository đơn lẻ không được tính là đã thực hiện. |

### 1.1 Nguyên tắc đánh giá

- Mã nguồn hiện tại là nguồn xác thực chính khi nội dung hai tài liệu MD cũ mâu thuẫn với code.
- Module có BE nhưng chưa có FE được đánh dấu **HOÀN THÀNH MỘT PHẦN** ở mức toàn hệ thống.
- Giao diện chỉ dùng dữ liệu hard-code hoặc mock không được tính là đã tích hợp.
- Dependency đã khai báo nhưng chưa có cấu hình hoặc luồng sử dụng không được tính là đã triển khai.
- Test inventory chỉ kiểm tra sự tồn tại endpoint không thay thế cho test nghiệp vụ end-to-end.

---

## 2. Tổng quan hệ thống

CinemaAI / CinePremier là hệ thống web đặt vé xem phim trực tuyến chỉ phục vụ **một rạp duy nhất**. Khách vãng lai có thể xem phim, suất chiếu, seat map và danh mục công khai. Khách hàng đã đăng nhập có thể giữ ghế, chọn loại vé và đồ ăn, thanh toán và nhận mã vé. Staff thực hiện check-in tại rạp duy nhất này. Admin quản lý danh mục, phòng chiếu, ghế, suất chiếu và hoạt động vận hành của rạp.

### 2.1 Quy tắc single-cinema bắt buộc

- Toàn hệ thống chỉ được tồn tại tối đa một bản ghi `Cinema`.
- Người dùng không chọn thành phố, chi nhánh hoặc rạp trước khi đặt vé.
- GUEST được xem seat map công khai, nhưng phải đăng nhập với role `CUSTOMER` trước khi hold ghế hoặc tạo booking.
- Mọi phòng chiếu, ghế, suất chiếu, booking, nhân viên và báo cáo đều thuộc rạp duy nhất.
- Rạp duy nhất phải được cấu hình sẵn bằng seed data hoặc database migration.
- Admin được xem, cập nhật, vô hiệu hóa và kích hoạt lại rạp.
- Admin không được tạo mới hoặc xóa rạp.
- Không có nghiệp vụ chuyển phòng, suất chiếu, nhân viên hoặc booking giữa các rạp.
- Báo cáo chỉ thống kê toàn rạp hoặc phân tích theo phòng, phim, suất chiếu và khoảng thời gian.
- Mọi danh sách nhiều địa điểm hoặc báo cáo theo thành phố trên frontend được xem là dữ liệu mock ngoài phạm vi và phải loại bỏ.

### 2.2 Công nghệ hiện tại

| Thành phần | Công nghệ |
|---|---|
| Frontend | React 19, React Router, Vite, Tailwind CSS |
| Backend | Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA |
| Xác thực | JWT access token, refresh token, Google login, email OTP |
| Cơ sở dữ liệu runtime | PostgreSQL (MySQL chỉ là cấu hình chạy local trên máy dev) |
| Test backend | JUnit, Spring Boot Test, H2 |
| Thanh toán | VNPay sandbox và mock payment |
| Lưu trữ ảnh | Cloudinary |
| Tài liệu API | Springdoc OpenAPI / Swagger |

### 2.3 Phạm vi không bao gồm

- Ứng dụng mobile native.
- POS bán vé trực tiếp tại quầy.
- Tích hợp phần cứng máy quét QR chuyên dụng.
- Mọi chức năng chuỗi rạp hoặc nhiều chi nhánh.
- Chọn rạp/thành phố khi xem lịch chiếu hoặc đặt vé.
- Báo cáo so sánh doanh thu giữa các rạp hoặc khu vực.
- Hệ thống kế toán hoặc đối soát thanh toán chuyên sâu.
- Audit log và màn hình tra cứu lịch sử thao tác quản trị.
- Promotion/khuyến mãi không nằm trong phạm vi nghiệp vụ hiện tại; hệ thống chỉ giữ wishlist ở nhóm engagement.

---

## 3. Actors và phân quyền

| Actor | Quyền chính | Trạng thái |
|---|---|---|
| **GUEST** | Xem phim, thể loại, diễn viên, rạp, suất chiếu, ghế, đồ ăn công khai | **ĐÃ HOÀN THÀNH** |
| **CUSTOMER** | Quản lý hồ sơ, wishlist, booking, thanh toán, vé, loyalty, recommendation | **HOÀN THÀNH MỘT PHẦN** |
| **STAFF** | Check-in vé tại rạp | **HOÀN THÀNH MỘT PHẦN** |
| **ADMIN** | Quản lý phim, diễn viên, thể loại, rạp, phòng, ghế, suất chiếu, tài khoản và booking | **HOÀN THÀNH MỘT PHẦN** |
| **SYSTEM** | Tự động giải phóng hold hết hạn, cập nhật trạng thái nền, cộng/hoàn điểm loyalty, tạo notification theo sự kiện | **HOÀN THÀNH MỘT PHẦN** |

Quy tắc RBAC nghiệp vụ:

- `/api/v1/admin/**`: yêu cầu role `ADMIN`.
- `/api/v1/staff/**`: theo nghiệp vụ chỉ yêu cầu role `STAFF`; `ADMIN` không có quyền check-in.
- API công khai chỉ cho phép các endpoint đọc danh mục cần thiết.
- API công khai chỉ cho phép các endpoint đọc danh mục cần thiết.

---

## 4. Kiến trúc và thành phần

### 4.1 Kiến trúc tổng quát

```text
React Frontend
    |
    | REST/JSON + JWT
    v
Spring Boot Backend
    |
    +-- PostgreSQL / JPA
    +-- VNPay Sandbox
    +-- Cloudinary
    +-- SMTP Email
```

### 4.2 Quy mô mã nguồn tại thời điểm đối chiếu

- 33 controller backend.
- 44 entity backend.
- 43 JPA repository được Spring phát hiện khi chạy test.
- 23 trang/panel frontend.
- 52 test backend chạy thành công.

### 4.3 Máy trạng thái nghiệp vụ

**Booking**

```text
HOLDING
  |-- xác nhận booking/thanh toán --> PENDING_PAYMENT
  |-- quá hạn ---------------------> EXPIRED
  |-- hủy -------------------------> CANCELLED

PENDING_PAYMENT
  |-- thanh toán thành công -------> PAID
  |-- quá hạn ---------------------> EXPIRED
  |-- hủy -------------------------> CANCELLED

PAID
  |-- staff check-in -------------> USED
  |-- admin hủy suất do sự cố ----> REFUNDED
```

**Suất chiếu**

```text
SCHEDULED --> OPEN --> COMPLETED
     |          |
     +----------+--> CANCELLED
```

**Ghế trong suất chiếu**

```text
AVAILABLE --> HOLDING --> BOOKED --> CHECKED_IN
                 |
                 +------> RELEASED
```

**Thanh toán**

```text
PENDING --> SUCCESS
    |
    +-----> FAILED
```

### 4.4 Thực thể dữ liệu cốt lõi

| Nhóm | Thực thể chính |
|---|---|
| Identity | `User`, `UserProfile`, `Role`, `UserRole`, `RefreshToken`, các token OTP/reset |
| Catalog | `Movie`, `Genre`, `MovieGenre`, `Actor`, `MovieActor`, `Review`, `Wishlist` |
| Cinema | `Cinema`, `Room`, `SeatRow`, `Seat`, `Showtime` |
| Commerce | `Booking`, `BookingSeat`, `BookingTicket`, `BookingFoodItem`, `Payment` |
| Loyalty/engagement | `LoyaltyPoint`, `Notification`, `TrailerInteraction`, `UserPreferenceProfile` |
| Operations | `StaffProfile`, `StaffShift`, `UploadedFile` |

---

## 5. Yêu cầu chức năng và trạng thái triển khai

## 5.1 Shared Foundation và chuẩn API

| ID | Yêu cầu | BE | FE | Tổng thể | Ghi chú |
|---|---|---|---|---|---|
| FND-01 | REST API theo version `/api/v1` | Hoàn thành | Tích hợp | **ĐÃ HOÀN THÀNH** | Phần lớn API dùng `/api/v1`. |
| FND-02 | Response/error format thống nhất | Hoàn thành | Có parse lỗi chung | **ĐÃ HOÀN THÀNH** | Có `ApiResponse`, `ErrorResponse`, `GlobalExceptionHandler`. |
| FND-03 | Correlation ID và request logging | Có | Không áp dụng | **ĐÃ HOÀN THÀNH** | Có filter tương ứng. |
| FND-04 | Swagger/OpenAPI và bearer JWT | Có | Không áp dụng | **ĐÃ HOÀN THÀNH** | Chưa xác minh độ đầy đủ của ví dụ response cho mọi API. |
| FND-05 | Health/actuator | Có | Không áp dụng | **ĐÃ HOÀN THÀNH** | Đã expose health và info. |

## 5.2 Database migration và seed data

| ID | Yêu cầu | Trạng thái | Ghi chú |
|---|---|---|---|
| DB-01 | Có schema cho các module chính | **ĐÃ HOÀN THÀNH** | Entity bao phủ auth, movie, cinema, booking, payment, loyalty, review và notification. |
| DB-02 | Migration có version | **HOÀN THÀNH MỘT PHẦN** | Có `V1` đến `V5`, nhưng Flyway đang tắt. |
| DB-03 | Migration tương thích database runtime | **CHƯA THỰC HIỆN** | Runtime dùng PostgreSQL nhưng migration nền dùng cú pháp SQL Server như `dbo`, `IDENTITY`, `NVARCHAR`. |
| DB-04 | Seed role, admin và cấu hình rạp cơ bản | **ĐÃ HOÀN THÀNH** | Không seed phim hoặc suất chiếu mẫu vào dữ liệu runtime. |
| DB-05 | Production dùng migration thay cho Hibernate update | **CHƯA THỰC HIỆN** | Hiện `spring.jpa.hibernate.ddl-auto=update`, `spring.flyway.enabled=false`. |

**Yêu cầu hoàn thiện:** chuyển toàn bộ migration sang PostgreSQL, bật Flyway và dùng `ddl-auto=validate` cho môi trường production.

## 5.3 Xác thực, tài khoản và hồ sơ người dùng

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| AUTH-01 | Đăng ký tài khoản và xác minh email OTP | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-02 | Gửi lại OTP xác minh email | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-03 | Đăng nhập email/mật khẩu | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-04 | Đăng nhập Google và xác minh bổ sung | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-05 | Refresh access token | Có | Có, tự refresh | **ĐÃ HOÀN THÀNH** |
| AUTH-06 | Đăng xuất và revoke refresh token | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-07 | Quên và đặt lại mật khẩu | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-08 | Xem/cập nhật hồ sơ cá nhân | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-09 | Upload avatar cá nhân | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-10 | Đổi mật khẩu khi đã đăng nhập | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-11 | Admin xem và khóa/mở tài khoản | Có | Có | **ĐÃ HOÀN THÀNH** |
| AUTH-12 | Admin tạo/cấp tài khoản STAFF | Có | Có | **ĐÃ HOÀN THÀNH** |

## 5.4 Phim, thể loại và diễn viên

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| MOV-01 | Public list/search/filter phim | Có | Có | **ĐÃ HOÀN THÀNH** |
| MOV-02 | Public xem chi tiết phim | Có | Có | **ĐÃ HOÀN THÀNH** |
| MOV-03 | Admin CRUD phim và đổi trạng thái | Có | Có | **ĐÃ HOÀN THÀNH** |
| MOV-04 | Quan hệ phim - thể loại | Có | Có trong quản lý phim | **ĐÃ HOÀN THÀNH** |
| MOV-05 | Public list/detail thể loại | Có | Có dùng trong lọc phim | **ĐÃ HOÀN THÀNH** |
| MOV-06 | Admin CRUD thể loại | Có | Có | **ĐÃ HOÀN THÀNH** |
| MOV-07 | Public list/detail/filmography diễn viên | Có | Có dữ liệu tích hợp một phần | **HOÀN THÀNH MỘT PHẦN** |
| MOV-08 | Admin CRUD diễn viên và gán diễn viên chính | Có | Có | **ĐÃ HOÀN THÀNH** |
| MOV-09 | Trailer interaction được gửi về recommendation | Có API | Chưa thấy FE gọi API interaction | **HOÀN THÀNH MỘT PHẦN** |

Lưu ý nghiệp vụ quản lý phim: Nếu phim đã qua ngày phát hành, ADMIN không được chuyển trạng thái ngược lại về `UPCOMING`; phim đã `ENDED` cũng không được lùi về `NOW_SHOWING` hoặc `UPCOMING`. Form quản trị phim cho phép nhập URL hoặc upload ảnh local cho poster và banner qua API upload ảnh. Trailer cho phép nhập URL ngoài hoặc upload video local lên Cloudinary qua API upload video, sau đó lưu URL trả về vào `trailerUrl`. Tất cả field tạo/cập nhật phim được validate ở BE và FE phải hiển thị thông báo lỗi field-level trả về từ API. Rule nhập phim: tên phim tối đa 50 ký tự, tên tiếng Anh/tiêu đề gốc tối đa 30 ký tự, mô tả tối đa 1000 ký tự, trailer URL bắt buộc, thời lượng từ 60 đến 180 phút, ngôn ngữ tối đa 30 ký tự, phụ đề tối đa 30 ký tự và đạo diễn tối đa 50 ký tự.

## 5.5 Rạp, phòng và sơ đồ ghế

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| CIN-01 | Public xem thông tin rạp duy nhất và các phòng | Có | Có | **ĐÃ HOÀN THÀNH** |
| CIN-02 | Admin lấy thông tin rạp duy nhất | Có `GET /api/v1/admin/cinema` | Có gọi API GET | **ĐÃ HOÀN THÀNH** |
| CIN-03 | Admin cập nhật thông tin rạp duy nhất | Có `PUT /api/v1/admin/cinema` | Có form và gọi API PUT | **ĐÃ HOÀN THÀNH** |
| CIN-04 | Admin vô hiệu hóa hoặc kích hoạt lại rạp | Có `PATCH /api/v1/admin/cinema/status` | Có nút tạm ẩn/hiển thị lại rạp | **ĐÃ HOÀN THÀNH** |
| CIN-05 | Không cho phép admin tạo mới hoặc xóa rạp | Không có `POST` và `DELETE` | Không có UI tạo mới hoặc xóa rạp | **ĐÃ HOÀN THÀNH** |
| CIN-06 | Frontend khách hàng chỉ hiển thị rạp duy nhất, không có nội dung nhiều địa điểm | Có API trả một rạp active | Header, footer, dashboard và lịch sử vé lấy rạp duy nhất từ API; đã xóa dữ liệu mock nhiều địa điểm | **ĐÃ HOÀN THÀNH** |
| CIN-07 | Admin CRUD và đổi trạng thái phòng | Có | Có | **ĐÃ HOÀN THÀNH** |
| CIN-08 | Sinh sơ đồ ghế theo hàng/cột | Có | Có | **ĐÃ HOÀN THÀNH** |
| CIN-09 | Thay thế layout ghế | Có | Có | **ĐÃ HOÀN THÀNH** |
| CIN-10 | Cập nhật/vô hiệu hóa ghế riêng lẻ | Có | Có | **ĐÃ HOÀN THÀNH** |
| CIN-11 | Chặn sửa layout khi có suất/booking đang hoạt động | Có kiểm tra nghiệp vụ | Phụ thuộc lỗi từ BE | **ĐÃ HOÀN THÀNH** |

## 5.6 Suất chiếu

| ID | Yêu cầu                                                                                          | BE | FE | Tổng thể |
|---|--------------------------------------------------------------------------------------------------|---|---|---|
| SHOW-01 | Public tra cứu suất chiếu                                                                        | Có | Có | **ĐÃ HOÀN THÀNH** |
| SHOW-02 | Public xem seat map của suất chiếu                                                               | Có | Có | **ĐÃ HOÀN THÀNH** |
| SHOW-03 | Admin tạo suất chiếu đơn                                                                         | Có | Có | **ĐÃ HOÀN THÀNH** |
| SHOW-04 | Admin tạo bulk suất chiếu                                                                        | Có | Có | **ĐÃ HOÀN THÀNH** |
| SHOW-05 | Kiểm tra xung đột phòng và thời gian                                                             | Có | Hiển thị kết quả từ BE | **ĐÃ HOÀN THÀNH** |
| SHOW-06 | Admin cập nhật trạng thái và hủy suất do sự cố rạp | Cần đồng bộ để cho phép hủy suất đã bán vé | Cần hiển thị cảnh báo refund wallet tự động | **HOÀN THÀNH MỘT PHẦN** |
| SHOW-07 | Khi admin hủy suất đã bán vé, hệ thống tự notification, hoàn tiền vào wallet và thu hồi loyalty | Chưa có luồng tự động đầy đủ | Chưa có UI xác nhận đầy đủ | **CHƯA THỰC HIỆN** |
| SHOW-08 | Scheduler tự chuyển `SCHEDULED -> OPEN -> COMPLETED`                                             | Chưa có | Chưa có | **CHƯA THỰC HIỆN** |

Lưu ý: code hiện dùng trạng thái suất chiếu `SCHEDULED`, `OPEN`, `CANCELLED`, `COMPLETED`; không dùng `NOW_SHOWING`.
Lưu ý nghiệp vụ: CUSTOMER không được yêu cầu hoàn tiền sau khi đã mua vé. Admin chỉ hủy suất chiếu đã bán vé khi có sự cố từ rạp; khi admin xác nhận hủy, hệ thống tự động gửi notification, hoàn tiền vào wallet của CUSTOMER và thu hồi/trừ loyalty đã cộng từ booking đó.

## 5.7 Giá vé

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| PRICE-01 | Giá theo loại vé ADULT/CHILD/STUDENT | Có | Có trong booking | **ĐÃ HOÀN THÀNH** |
| PRICE-02 | Giá theo loại ghế STANDARD/VIP/COUPLE | Có | Có trong booking | **ĐÃ HOÀN THÀNH** |
| PRICE-03 | Phụ phí cuối tuần/ngày lễ | Có | Có gửi cờ holiday | **ĐÃ HOÀN THÀNH** |
| PRICE-04 | Kiểm tra tuổi theo age rating và loại vé | Có | Có nhập tuổi/chọn vé | **ĐÃ HOÀN THÀNH** |
| PRICE-05 | Admin cấu hình quy tắc giá vé theo loại khách và loại ghế | Có | Có trực tiếp trong màn hình tạo/cập nhật suất chiếu | **ĐÃ HOÀN THÀNH** |


## 5.8 Đồ ăn và combo F&B

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| FOOD-01 | Public xem món ăn và combo | Có | Có | **ĐÃ HOÀN THÀNH** |
| FOOD-02 | Admin CRUD món ăn và combo | Có | Có | **ĐÃ HOÀN THÀNH** |
| FOOD-03 | Chọn đồ ăn trong booking | Có | Có | **ĐÃ HOÀN THÀNH** |
| FOOD-04 | Quản lý tồn kho thực tế | Chỉ có trạng thái | Chưa có | **CHƯA THỰC HIỆN** |

## 5.9 Booking và giữ ghế

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| BOOK-01 | Chọn suất chiếu và tải seat map | Có | Có | **ĐÃ HOÀN THÀNH** |
| BOOK-02 | Hold nhiều ghế trong 2 phút | Có | Có | **ĐÃ HOÀN THÀNH** |
| BOOK-03 | Chặn ghế đã HOLDING/BOOKED/CHECKED_IN | Có kiểm tra | Có phản hồi lỗi | **ĐÃ HOÀN THÀNH** |
| BOOK-04 | Database lock chống hai request giữ cùng ghế | Chưa thấy pessimistic lock/unique active constraint | Không áp dụng | **CHƯA THỰC HIỆN** |
| BOOK-05 | Tạo booking từ hold | Có | Có | **ĐÃ HOÀN THÀNH** |
| BOOK-06 | Chọn loại vé, tuổi và F&B | Có | Có | **ĐÃ HOÀN THÀNH** |
| BOOK-07 | Xem danh sách/chi tiết booking cá nhân | Có | Có | **ĐÃ HOÀN THÀNH** |
| BOOK-08 | Hủy booking hợp lệ | Có | FE chưa có luồng rõ ràng | **HOÀN THÀNH MỘT PHẦN** |
| BOOK-09 | Scheduler giải phóng hold hết hạn | Có | Không áp dụng | **ĐÃ HOÀN THÀNH** |
| BOOK-10 | Realtime cập nhật trạng thái ghế | Chưa có | Chưa có | **CHƯA THỰC HIỆN** |

Rủi ro hiện tại: quy trình hold chạy trong transaction nhưng chưa có database row lock hoặc ràng buộc duy nhất đảm bảo tuyệt đối khi hai request đồng thời giữ cùng một ghế.
Quy tắc ranh giới GUEST/CUSTOMER: GUEST được xem phim, suất chiếu và seat map; CUSTOMER bắt buộc đăng nhập trước khi hold ghế, tạo booking, thanh toán hoặc xem vé.
Quy tắc hủy/hoàn tiền: booking `HOLDING` hoặc `PENDING_PAYMENT` có thể hủy nếu còn hợp lệ; booking `PAID` là vé đã bán ra và CUSTOMER không được yêu cầu hoàn tiền. Chỉ khi ADMIN hủy suất do sự cố từ rạp, hệ thống mới chuyển booking đã thanh toán sang `REFUNDED`, hoàn tiền vào wallet của CUSTOMER, gửi notification và thu hồi/trừ loyalty.

## 5.10 Wishlist

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| WISH-01 | Thêm/xóa/xem wishlist | Có | Có trang wishlist | **ĐÃ HOÀN THÀNH** |
| WISH-02 | Thông báo khi phim wishlist có suất mới | Chưa có luồng sự kiện | Chưa có | **CHƯA THỰC HIỆN** |

Lưu ý phạm vi: Promotion/khuyến mãi đã được loại khỏi nghiệp vụ hiện tại, không bổ sung luồng áp dụng mã giảm giá trong tài liệu này.

## 5.11 Thanh toán và hoàn tiền do hủy suất

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| PAY-01 | Tạo URL thanh toán VNPay | Có | Có | **ĐÃ HOÀN THÀNH** |
| PAY-02 | Xử lý VNPay return và IPN | Có | Có callback page | **ĐÃ HOÀN THÀNH** |
| PAY-03 | Xác minh chữ ký và số tiền callback | Có | Không áp dụng | **ĐÃ HOÀN THÀNH** |
| PAY-04 | Idempotency khi callback lặp | Có kiểm tra payment success | Không áp dụng | **ĐÃ HOÀN THÀNH** |
| PAY-05 | Mock payment cho môi trường dev/demo | Có | Có, tự fallback khi VNPay lỗi | **ĐÃ HOÀN THÀNH** |
| PAY-06 | Tra cứu payment theo booking | Có | Chưa thấy UI dùng | **HOÀN THÀNH MỘT PHẦN** |
| PAY-07 | Không cho CUSTOMER yêu cầu refund sau khi đã mua vé | Cần chặn/loại luồng refund customer nếu còn tồn tại | Cần loại UI refund customer nếu còn tồn tại | **CHƯA ĐỒNG BỘ ĐẦY ĐỦ** |
| PAY-08 | Admin hủy suất do sự cố rạp để kích hoạt hoàn tiền tự động | Cần đồng bộ với showtime cancellation mới | Cần UI xác nhận lý do hủy suất | **CHƯA THỰC HIỆN** |
| PAY-09 | Hoàn tiền vào wallet CUSTOMER khi suất bị admin hủy | Chưa có wallet/ledger refund đầy đủ | Chưa có UI wallet rõ ràng | **CHƯA THỰC HIỆN** |
| PAY-10 | Đối soát và báo cáo giao dịch | Chưa có | Chưa có tích hợp thật | **CHƯA THỰC HIỆN** |

Quy tắc hoàn tiền: hệ thống không cho CUSTOMER gửi yêu cầu refund sau khi booking đã thanh toán. Hoàn tiền chỉ phát sinh khi ADMIN hủy suất chiếu vì sự cố từ rạp; hệ thống tự chuyển booking đã thanh toán của suất đó sang `REFUNDED`, hoàn tiền vào wallet của CUSTOMER, gửi notification và thu hồi/trừ loyalty đã cộng từ booking đó. Không dùng VNPay Refund API cho luồng khách tự hủy vé.

## 5.12 Vé QR và check-in staff

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| TICKET-01 | Sinh QR sau thanh toán | Có chuỗi QR | Có hiển thị vé | **ĐÃ HOÀN THÀNH** |
| TICKET-02 | QR có chữ ký/checksum chống giả mạo | Chưa có | Không áp dụng | **CHƯA THỰC HIỆN** |
| TICKET-03 | Staff check-in bằng QR | Có API chỉ cho `STAFF` | Có gọi API thật | **ĐÃ HOÀN THÀNH** |
| TICKET-04 | Chặn check-in vé chưa thanh toán/đã dùng | Có | Có hiển thị lỗi từ API thật | **ĐÃ HOÀN THÀNH** |
| TICKET-05 | Tra cứu booking thủ công cho staff | Có API staff lookup theo booking code/QR | Có form tra cứu thật | **ĐÃ HOÀN THÀNH** |
| TICKET-06 | Camera quét QR thực tế | Không áp dụng | Chỉ mô phỏng quét | **CHƯA THỰC HIỆN** |
| TICKET-07 | Gửi vé QR qua email | Chưa có | Chưa có | **CHƯA THỰC HIỆN** |

Lưu ý bảo mật: QR hiện có dạng `CINEAI:<bookingCode>:<userId>` và chưa có chữ ký hoặc checksum.
Lưu ý phạm vi check-in: QR hiện gắn với booking, nên một lần quét xác nhận toàn bộ booking. Nếu cần check-in từng vé/ghế riêng lẻ, hệ thống phải bổ sung QR và trạng thái riêng cho từng vé.

## 5.13 Loyalty

| ID | Yêu cầu                                                                                                     | BE | FE | Tổng thể |
|---|-------------------------------------------------------------------------------------------------------------|---|---|---|
| LOY-01 | Tự cộng điểm khi thanh toán thành công theo số ghế/vé trong booking | Có | Chưa hiển thị tích hợp rõ ràng | **HOÀN THÀNH MỘT PHẦN** |
| LOY-02 | CUSTOMER xem điểm hiện tại | Có API | Profile hiện chủ yếu hiển thị mockup điểm | **HOÀN THÀNH MỘT PHẦN** |
| LOY-03 | Thu hồi/trừ điểm đã cộng khi booking được hoàn tiền do admin hủy suất | Chưa rõ ledger hoàn điểm | Chưa có | **CHƯA THỰC HIỆN** |
| LOY-04 | Lịch sử giao dịch điểm chi tiết | Model hiện lưu tổng điểm, chưa đáp ứng ledger đầy đủ | Chưa có | **CHƯA THỰC HIỆN** |
| LOY-05 | Đổi điểm khi thanh toán với tỷ lệ 1000 điểm = 1.000 VND | Chưa có | Chưa có | **CHƯA THỰC HIỆN** |

Quy tắc loyalty: điểm tồn tại vĩnh viễn. Mỗi phim cấu hình số điểm cộng trên một ghế/vé; booking có nhiều ghế thì tổng điểm cộng bằng điểm của phim nhân với số ghế hợp lệ. Khi booking được hoàn tiền do ADMIN hủy suất, SYSTEM thu hồi/trừ phần điểm đã cộng từ booking đó theo ledger.

## 5.14 Notification

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| NOTI-01 | Tạo notification cho CUSTOMER | Có API/service thủ công | Chưa có UI | **HOÀN THÀNH MỘT PHẦN** |
| NOTI-02 | Xem tất cả/chưa đọc và đánh dấu đã đọc | Có API | Chưa có UI | **HOÀN THÀNH MỘT PHẦN** |
| NOTI-03 | Tự tạo notification từ booking/payment/hủy suất/showtime | Chưa thấy tích hợp service vào các luồng | Chưa có | **CHƯA THỰC HIỆN** |
| NOTI-04 | Đánh dấu tất cả đã đọc | Chưa có API | Chưa có | **CHƯA THỰC HIỆN** |
| NOTI-05 | WebSocket notification realtime | Chỉ có dependency WebSocket | Chưa có | **CHƯA THỰC HIỆN** |

## 5.15 Review phim

| ID | Yêu cầu | Trạng thái | Ghi chú |
|---|---|---|---|
| REV-01 | CUSTOMER tạo/sửa review sau khi xem | **CHƯA THỰC HIỆN** | Có entity/repository nhưng chưa có controller/service. Chỉ cho phép review khi CUSTOMER có booking `USED` của phim đó. |
| REV-02 | Public xem review công khai | **CHƯA THỰC HIỆN** | Review mới của CUSTOMER được auto accept/hiển thị ngay; FE không seed mock review nhưng chưa có API thật. |
| REV-03 | Admin ẩn/xóa review | **CHƯA THỰC HIỆN** | Không cần admin duyệt trước; admin chỉ ẩn hoặc xóa review vi phạm. Chưa có API và UI. |
| REV-04 | Tính điểm trung bình phim từ review | **CHƯA THỰC HIỆN** | Chưa có service tổng hợp. |
| REV-05 | Dùng review làm tín hiệu recommendation | **CHƯA THỰC HIỆN** | Recommendation service có cấu trúc liên quan nhưng chưa có review flow đầu vào. |

Quy tắc review: mỗi CUSTOMER chỉ được tạo một review cho mỗi phim sau khi đã check-in/xem phim thành công. Review mới được auto accept/hiển thị công khai ngay và được tính vào điểm trung bình; admin chỉ ẩn hoặc xóa review vi phạm.

## 5.16 Recommendation cá nhân hóa

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| REC-01 | Ghi nhận trailer interaction | Có | Chưa tích hợp | **HOÀN THÀNH MỘT PHẦN** |
| REC-02 | Refresh/xem hồ sơ sở thích | Có | Chưa có trang CUSTOMER | **HOÀN THÀNH MỘT PHẦN** |
| REC-03 | Gợi ý phim cá nhân hóa | Có, strategy hiện là mock/rule-based | Chưa có khu vực tích hợp rõ ràng | **HOÀN THÀNH MỘT PHẦN** |
| REC-04 | Gợi ý theo diễn viên yêu thích | Có | Chưa có UI | **HOÀN THÀNH MỘT PHẦN** |
| REC-05 | Admin debug recommendation | Có | Chưa có panel debug recommendation riêng | **HOÀN THÀNH MỘT PHẦN** |

## 5.17 Upload và lưu trữ

| ID | Yêu cầu | BE | FE | Tổng thể |
|---|---|---|---|---|
| UP-01 | Admin upload ảnh lên Cloudinary | Có | Có dùng cho phim/diễn viên/avatar | **ĐÃ HOÀN THÀNH** |
| UP-02 | Lưu metadata file upload | Có entity/repository/service upload | Không có UI quản lý | **HOÀN THÀNH MỘT PHẦN** |
| UP-03 | Admin list file đã upload | Chưa có API | Chưa có | **CHƯA THỰC HIỆN** |
| UP-04 | Xóa file và xóa trên Cloudinary | Chưa có API/service | Chưa có | **CHƯA THỰC HIỆN** |

## 5.18 Staff operations và báo cáo

| ID | Yêu cầu                                                    | Trạng thái | Ghi chú |
|---|------------------------------------------------------------|---|---|
| OPS-01 | Staff check-in QR                                          | **ĐÃ HOÀN THÀNH** | BE và FE đã dùng API thật; chỉ role `STAFF` được check-in. |
| OPS-02 | Staff xem booking theo suất chiếu                          | **ĐÃ HOÀN THÀNH** | BE có endpoint STAFF lấy booking theo `showtimeId`; FE Staff page đã gọi API thật và cho check-in từ danh sách. |
| OPS-03 | Quản lý staff profile cơ bản                              | **ĐÃ HOÀN THÀNH** | Admin có API/UI quản lý mã nhân viên, thông tin tài khoản STAFF, số điện thoại, vị trí, trạng thái và rạp duy nhất. |
| RPT-01 | Báo cáo doanh thu của rạp duy nhất theo ngày/tháng/quý/năm | **CHƯA THỰC HIỆN** | Admin overview hiện dùng dữ liệu tính cục bộ/mock. |
| RPT-02 | Báo cáo vé bán, phim bán chạy, suất chiếu                  | **CHƯA THỰC HIỆN** | Chưa có report API. |
| RPT-03 | Báo cáo combo bắp nước và tỷ lệ lấp đầy theo phòng         | **CHƯA THỰC HIỆN** | Chưa có report API. |
| RPT-04 | Export báo cáo                                             | **CHƯA THỰC HIỆN** | Chưa có. |
| RPT-05 | Loại bỏ báo cáo doanh thu theo nhiều thành phố/rạp         | **ĐÃ HOÀN THÀNH** | Admin overview chỉ hiển thị doanh thu của rạp duy nhất lấy từ API. |

---

## 6. Yêu cầu phi chức năng

| ID | Yêu cầu | Trạng thái | Đánh giá hiện tại |
|---|---|---|---|
| NFR-SEC-01 | JWT, refresh token và RBAC | **ĐÃ HOÀN THÀNH** | Có Spring Security, JWT filter và role rules. |
| NFR-SEC-02 | Hash mật khẩu bằng BCrypt | **ĐÃ HOÀN THÀNH** | Dùng BCrypt strength 10. |
| NFR-SEC-03 | Không lưu secret trực tiếp trong source | **CHƯA THỰC HIỆN** | `application.properties` đang chứa thông tin DB, SMTP và VNPay mặc định nhạy cảm. |
| NFR-SEC-04 | QR có chữ ký chống giả mạo | **CHƯA THỰC HIỆN** | QR là chuỗi có thể đoán/tạo lại. |
| NFR-DATA-01 | Transaction cho booking/payment | **ĐÃ HOÀN THÀNH** | Các service chính dùng `@Transactional`. |
| NFR-DATA-02 | Lock chống double booking | **CHƯA THỰC HIỆN** | Chưa có database locking/ràng buộc phù hợp. |
| NFR-DATA-03 | Idempotency callback payment | **ĐÃ HOÀN THÀNH** | Có guard khi payment đã success. |
| NFR-PERF-01 | Cache danh mục/seat map | **CHƯA THỰC HIỆN** | Chưa thấy cache provider/config. |
| NFR-PERF-02 | Scheduler cleanup theo batch | **HOÀN THÀNH MỘT PHẦN** | Có scheduler nhưng quét danh sách, chưa có batch/paging/lock phân tán. |
| NFR-OBS-01 | Logging và correlation ID | **ĐÃ HOÀN THÀNH** | Có filter và log service. |
| NFR-OBS-02 | Metrics/monitoring production | **HOÀN THÀNH MỘT PHẦN** | Có actuator cơ bản, chưa có dashboard/alert. |
| NFR-QA-01 | Backend integration tests | **ĐÃ HOÀN THÀNH** | 52 test hiện chạy thành công. |
| NFR-QA-02 | Frontend unit/component/E2E tests | **CHƯA THỰC HIỆN** | Chưa có test script/framework frontend. |
| NFR-QA-03 | Production build frontend | **ĐÃ HOÀN THÀNH** | Build thành công, nhưng bundle JS gần 1 MB và có cảnh báo chunk lớn. |

---

## 7. Ma trận trạng thái module

| Phase | Module | Trạng thái tổng thể | Lý do chính |
|---|---|---|---|
| Phase 0 | Shared Foundation | **ĐÃ HOÀN THÀNH** | API foundation, security, response/error format và logging đã có. |
| Phase 1 | Database Migration | **HOÀN THÀNH MỘT PHẦN** | Có file migration nhưng Flyway tắt và sai dialect runtime. |
| Phase 2 | Auth, Customer & Security | **ĐÃ HOÀN THÀNH** | Luồng CUSTOMER chính đã tích hợp FE-BE. |
| Phase 3 | Movie, Genre & Actor | **ĐÃ HOÀN THÀNH** | CRUD và public catalog đã tích hợp. |
| Phase 4 | Cinema, Room, Seat & Showtime | **HOÀN THÀNH MỘT PHẦN** | Quản lý rạp đúng phạm vi GET/UPDATE/PATCH status, không có CREATE/DELETE; FE đã chỉ dùng một địa điểm từ API, scheduler trạng thái suất còn thiếu; cần cho phép admin hủy suất do sự cố và kích hoạt refund wallet tự động. |
| Phase 5 | Booking, Seat Locking, F&B & QR | **HOÀN THÀNH MỘT PHẦN** | Booking và staff check-in hoạt động với API thật; còn thiếu lock đồng thời. |
| Phase 6 | Payment & Wallet Refund | **HOÀN THÀNH MỘT PHẦN** | VNPay/payment có; cần bỏ luồng customer refund và bổ sung hoàn tiền vào wallet khi admin hủy suất do sự cố rạp. |
| Phase 7 | Loyalty & Notification | **HOÀN THÀNH MỘT PHẦN** | Có API nền, FE và event integration còn thiếu; loyalty cần ledger để thu hồi/trừ điểm khi booking được hoàn tiền do hủy suất. |
| Phase 8 | Review | **CHƯA THỰC HIỆN** | Chỉ có entity/repository. |
| Phase 9 | Staff Operations & Reports | **HOÀN THÀNH MỘT PHẦN** | Staff check-in, danh sách booking theo suất chiếu và staff profile cơ bản đã dùng API thật; report API chưa có. |
| Phase 10 | Storage, Email, WebSocket & Scheduler | **HOÀN THÀNH MỘT PHẦN** | Upload và OTP mail có; ticket email/WebSocket/showtime scheduler thiếu. |
| Phase 11 | Recommendation | **HOÀN THÀNH MỘT PHẦN** | Recommendation backend có nền tảng rule-based; FE chưa tích hợp rõ ràng. |
| Phase 12 | Integration & QA | **HOÀN THÀNH MỘT PHẦN** | Backend test tốt; thiếu FE/E2E và kiểm thử tích hợp dịch vụ thật. |

---

## 8. Danh sách đã hoàn thành nổi bật

- Xác thực đầy đủ: đăng ký, OTP email, login, Google login, refresh, logout, reset và đổi mật khẩu.
- Quản lý hồ sơ, avatar và quản trị trạng thái CUSTOMER/STAFF.
- CRUD phim, thể loại, diễn viên và quan hệ phim - diễn viên/thể loại.
- Backend thực thi giới hạn một rạp; quản lý phòng, ghế, layout và suất chiếu của rạp đó.
- Public catalog, chi tiết phim, suất chiếu và seat map.
- Booking core: hold ghế, loại vé, kiểm tra tuổi, đồ ăn, tính giá và giải phóng hold.
- VNPay sandbox, callback/IPN, mock payment và sinh QR sau thanh toán.
- Wishlist cho CUSTOMER.
- Upload ảnh Cloudinary.
- 52 backend tests đang pass và frontend production build thành công.

---

## 9. Danh sách chưa hoàn thành hoặc cần làm tiếp

### 9.1 Ưu tiên P0 - Bắt buộc trước khi production

1. Sửa migration sang PostgreSQL, bật Flyway và loại bỏ `ddl-auto=update`.
2. Di chuyển toàn bộ DB/SMTP/VNPay/JWT secret khỏi source code và rotate secret đã lộ.
3. Thêm database lock hoặc unique constraint phù hợp để chống double booking.
4. Ký QR bằng HMAC/JWT hoặc checksum bí mật.
5. Tắt hoặc bảo vệ mock payment trong môi trường production.
6. Bổ sung test đồng thời giữ ghế và test end-to-end booking -> payment -> check-in.

### 9.2 Ưu tiên P1 - Hoàn thiện nghiệp vụ bàn giao

1. Xây dựng ReviewController, ReviewService, moderation và điểm trung bình phim.
2. Bỏ luồng CUSTOMER yêu cầu refund sau khi mua vé; bổ sung wallet refund tự động khi ADMIN hủy suất do sự cố rạp.
3. Tạo report API cho doanh thu, vé bán, phim bán chạy và tỷ lệ lấp đầy.
4. Tích hợp loyalty và notification vào frontend.
5. Tự tạo notification từ payment, cancel trước thanh toán, hủy suất và hoàn tiền wallet.
6. Gửi email xác nhận booking và vé QR.
7. Thêm scheduler tự động chuyển trạng thái suất chiếu.
8. Hoàn thiện admin flow hủy suất do sự cố, xác nhận lý do hủy và theo dõi refund wallet.

### 9.3 Ưu tiên P2 - Nâng cao trải nghiệm

1. WebSocket cho seat map và notification realtime.
2. Tích hợp recommendation vào trang người dùng.
3. Quản lý file upload, xóa Cloudinary và quản lý storage.
4. Quản lý staff profile và ca làm.
5. Thêm frontend unit test, component test và E2E test.
6. Code splitting frontend để giảm bundle hiện gần 1 MB.

---

## 10. Tiêu chí nghiệm thu tổng thể

Hệ thống được xem là sẵn sàng bàn giao production khi:

- Toàn bộ yêu cầu P0 được hoàn thành.
- Không thể giữ hoặc thanh toán trùng một ghế trong cùng suất chiếu khi có request đồng thời.
- Mọi API admin/staff/customer/system-facing được kiểm tra đúng RBAC.
- VNPay callback và payment reconciliation được kiểm thử trên sandbox; refund wallet do admin hủy suất được kiểm thử bằng ledger nội bộ.
- QR không thể tự tạo hợp lệ nếu không có secret của hệ thống.
- Staff check-in sử dụng API và dữ liệu thật.
- Migration có thể dựng database PostgreSQL mới từ đầu.
- Có E2E test cho auth, booking, payment, check-in, cancel trước thanh toán và refund wallet do admin hủy suất.
- Secret production chỉ được cấp qua biến môi trường hoặc secret manager.
- Không còn dữ liệu mock xuất hiện trong luồng nghiệp vụ production.
- Rạp duy nhất được cấu hình sẵn; API và frontend quản trị rạp chỉ cho phép GET, UPDATE và PATCH trạng thái.
- API tạo mới và xóa rạp không tồn tại.
- Frontend không còn chức năng chọn rạp/thành phố.
- Dashboard và báo cáo chỉ sử dụng dữ liệu của rạp duy nhất.

---

## 11. Kết quả xác minh ngày 15/06/2026

| Hạng mục | Kết quả |
|---|---|
| Backend test | **PASS** - 52 tests, 0 failures, 0 errors |
| Frontend build | **PASS** - Vite production build thành công |
| Frontend warning | Bundle JS khoảng 976 KB trước gzip, cần code splitting |
| Kiểm thử PostgreSQL production | Chưa thực hiện trong lần đối chiếu này |
| Kiểm thử dịch vụ thật VNPay/SMTP/Cloudinary | Chưa thực hiện trong lần đối chiếu này |
| Kiểm thử E2E trên trình duyệt | Chưa có bộ test tự động |

---

## 12. Kết luận

Hệ thống hiện đã hoàn thành tốt phần lõi của website đặt vé cho một rạp duy nhất: auth, catalog, cập nhật và vô hiệu hóa/kích hoạt lại rạp, quản trị phòng, ghế, suất chiếu, booking, tính giá, F&B, payment và staff check-in. Quản lý rạp hiện đúng phạm vi: có GET, UPDATE và PATCH trạng thái; không có API/UI tạo mới hoặc xóa rạp. Frontend đã loại bỏ nội dung mock nhiều địa điểm và lấy thông tin rạp duy nhất từ API; hệ thống chưa nên được xem là hoàn chỉnh ở mức production do còn thiếu cơ chế chống double booking ở database, migration đúng chuẩn PostgreSQL, bảo mật QR/secret, review, báo cáo, notification event-driven và wallet refund tự động khi admin hủy suất do sự cố rạp.

Tài liệu này là baseline SRS và trạng thái triển khai để tiếp tục phát triển, kiểm thử và nghiệm thu dự án.
