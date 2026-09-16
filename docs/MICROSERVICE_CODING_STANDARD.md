# QUY CHUẨN PHÁT TRIỂN MICROSERVICES (MICROSERVICE CODING STANDARD)
**Dự án**: CinemaAI Platform  
**Phiên bản**: 1.0  
**Áp dụng cho**: Toàn bộ các dịch vụ Backend (`cinema-services/*`) bao gồm `api-gateway`, `identity-service`, `catalog-service`, `booking-service`, `payment-service`.

---

## MỤC LỤC
1. [Chuẩn 1: Quản lý cấu hình & Biến môi trường (.env & application.properties)](#1-chuẩn-1-quản-lý-cấu-hình--biến-môi-trường-env--applicationproperties)
2. [Chuẩn 2: Kiến trúc Gateway & Bảo mật mạng (Network Isolation & Port Locking)](#2-chuẩn-2-kiến-trúc-gateway--bảo-mật-mạng-network-isolation--port-locking)
3. [Chuẩn 3: Thiết kế mã nguồn (Interface-Driven Development & Clean Architecture)](#3-chuẩn-3-thiết-kế-mã-nguồn-interface-driven-development--clean-architecture)
4. [Chuẩn 4: Thiết kế DTO & Xác thực dữ liệu (Request Validation & Immutability)](#4-chuẩn-4-thiết-kế-dto--xác-thực-dữ-liệu-request-validation--immutability)
5. [Chuẩn 5: Chuẩn hóa phản hồi API & Xử lý ngoại lệ (Envelope & Exception Handling)](#5-chuẩn-5-chuẩn-hóa-phản-hồi-api--xử-lý-ngoại-lệ-envelope--exception-handling)
6. [Chuẩn 6: Độc lập CSDL & Quản lý Migration (Database-per-Service & Flyway)](#6-chuẩn-6-độc-lập-csdl--quản-lý-migration-database-per-service--flyway)
7. [Chuẩn 7: Truy vết phân tán & Ghi log (Distributed Tracing & Correlation ID)](#7-chuẩn-7-truy-vết-phân-tán--ghi-log-distributed-tracing--correlation-id)
8. [Chuẩn 8: Quy ước đặt tên & Thiết kế RESTful URI](#8-chuẩn-8-quy-ước-đặt-tên--thiết-kế-restful-uri)

---

## 1. Chuẩn 1: Quản lý cấu hình & Biến môi trường (.env & application.properties)

### 1.1. Nguyên tắc "Zero-Default-Value" trong file cấu hình
- **Quy tắc bắt buộc**: Toàn bộ giá trị trong `application.properties` (hoặc `application.yml`) **BẮT BUỘC** phải tham chiếu tới biến môi trường thông qua cú pháp thuần:
  $$\text{property.key}=\$\{\text{VARIABLE\_NAME}\}$$
- **NGHIÊM CẤM tuyệt đối**: Không được gắn giá trị mặc định sau dấu hai chấm `:`.
  - ❌ **SAI**:
    ```properties
    server.port=${SERVER_PORT:8081}
    spring.datasource.password=${DB_PASSWORD:secret123}
    app.jwt.access-expiration-ms=${JWT_ACCESS_EXPIRATION_MS:1800000}
    app.mail.enabled=${MAIL_ENABLED:true}
    ```
  - ✅ **ĐÚNG**:
    ```properties
    server.port=${SERVER_PORT}
    spring.datasource.password=${DB_PASSWORD}
    app.jwt.access-expiration-ms=${JWT_ACCESS_EXPIRATION_MS}
    app.mail.enabled=${MAIL_ENABLED}
    ```
- **Lý do**:
  1. Tránh việc ứng dụng âm thầm chạy với giá trị mặc định (fallback) không mong muốn, gây sai lệch logic môi trường dev/staging/prod hoặc lộ thông tin nhạy cảm.
  2. Bắt buộc hệ thống tuân thủ nguyên tắc **Fail-Fast**: Nếu lập trình viên hoặc CI/CD quên khai báo bất kỳ biến môi trường nào, Spring Boot sẽ dừng khởi động ngay lập tức kèm log lỗi rõ ràng (`Could not resolve placeholder`).

### 1.2. Quy định về file `.env` và `.env.example`
- Mỗi service phải có một file `.env.example` được commit lên Git làm tài liệu mẫu cho tất cả các biến môi trường mà service đó yêu cầu.
- File `.env` chứa giá trị thực tế (chạy local hoặc production) và **BẮT BUỘC phải nằm trong `.gitignore`**, tuyệt đối không push lên Git.
- Cú pháp tên biến: Viết hoa toàn bộ, phân cách bằng dấu gạch dưới `_` (SCREAMING_SNAKE_CASE).
  - Ví dụ: `SERVER_PORT`, `DB_URL`, `JWT_SECRET`, `RABBITMQ_HOST`.

---

## 2. Chuẩn 2: Kiến trúc Gateway & Bảo mật mạng (Network Isolation & Port Locking)

### 2.1. Điểm truy cập duy nhất (Single Entry Point)
- **Cổng duy nhất được công khai (Public)**: API Gateway chạy trên cổng **`8080`**.
- Mọi Client (Web Frontend, Mobile App, Postman, đối tác thứ ba) **BẮT BUỘC** phải gọi qua `http://<domain_or_host>:8080`.
- Không một client nào được phép gọi trực tiếp tới cổng nội bộ của các microservice (`8081`, `8082`, `8083`, `8084`, `8000`).

### 2.2. Cơ chế chặn truy cập trực tiếp vào Microservices (Port Locking)
Để ngăn chặn tấn công bypass Gateway hoặc truy cập trái phép, dự án áp dụng mô hình bảo vệ 2 lớp (**Defense in Depth**):

1. **Lớp 1: Khóa ở mức Mạng (Network / Docker Isolation)**
   - Trong môi trường Docker Compose / Kubernetes: Các microservice backend nằm hoàn toàn trong mạng nội bộ (`internal-network`), **không dùng directive `ports:`** để export port ra host máy chủ.
   - Chỉ duy nhất container `api-gateway` được khai báo `ports: ["8080:8080"]` để mở ra ngoài Internet/Host.
   - Khi chạy local ngoài Docker: Các microservice con chỉ bind vào interface loopback `127.0.0.1` thay vì `0.0.0.0` (`server.address=127.0.0.1`).

2. **Lớp 2: Khóa ở mức Ứng dụng (Gateway Secret Token Header)**
   - API Gateway tự động sinh hoặc cấu hình 1 mã bí mật nội bộ `INTERNAL_GATEWAY_SECRET` (khai báo trong `.env`).
   - Mọi request khi đi qua Gateway sẽ được Gateway tự động đính kèm thêm header nội bộ:
     ```http
     X-Gateway-Secret: ${INTERNAL_GATEWAY_SECRET}
     ```
   - Tất cả các Microservice con cài đặt một `GatewaySecurityFilter` (hoặc cấu hình Security):
     - Nếu request **không có** hoặc **sai** header `X-Gateway-Secret`, microservice sẽ lập tức từ chối với mã phản hồi **`403 Forbidden`** và log cảnh báo an ninh.
     - Điều này đảm bảo dù kẻ tấn công có quét thấy cổng của service nội bộ cũng không thể gửi request trực tiếp được.

---

## 3. Chuẩn 3: Thiết kế mã nguồn (Interface-Driven Development & Clean Architecture)

### 3.1. Nguyên tắc Interface-Driven Development
Mọi tầng xử lý nghiệp vụ (`service`) **BẮT BUỘC** phải phân tách thành 2 phần:
1. **Interface**: Khai báo hợp đồng hành vi (Contract), đặt trong package `service/`.
2. **Implementation Class**: Cài đặt logic chi tiết, đặt trong package `service/impl/` với hậu tố `ServiceImpl`.

**Ví dụ**:
- `com.cinemaai.identity.service.UserService` (Interface)
- `com.cinemaai.identity.service.impl.UserServiceImpl` (Class có `@Service`)

### 3.2. Quy định Dependency Injection
- Các Controller, Filter, hoặc Service khác khi phụ thuộc vào một Service **CHỈ ĐƯỢC PHÉP tiêm (inject) Interface**, không bao giờ tiêm Class cài đặt (`ServiceImpl`).
- Ưu tiên tiêm phụ thuộc qua Constructor (Constructor Injection) kết hợp Lombok `@RequiredArgsConstructor(onConstructor_ = @Autowired)` hoặc viết constructor tường minh.
  ```java
  // ✅ ĐÚNG: Tiêm interface qua constructor
  @RestController
  @RequiredArgsConstructor
  public class UserController {
      private final UserService userService; // Inject Interface
  }

  // ❌ SAI: Tiêm trực tiếp class Impl
  @RestController
  public class UserController {
      private UserServiceImpl userService;
  }
  ```

---

## 4. Chuẩn 4: Thiết kế DTO & Xác thực dữ liệu (Request Validation & Immutability)

### 4.1. Sử dụng Java `record` cho toàn bộ DTO
- Toàn bộ Request DTO và Response DTO phải được định nghĩa bằng **Java `record`** thay vì Class thông thường.
- `record` đảm bảo tính bất biến (Immutable), cú pháp ngắn gọn, tự động sinh `equals`, `hashCode`, `toString` và không bị lỗi lộ dữ liệu qua setter.

### 4.2. Xác thực chặt chẽ bằng Jakarta Validation
- Mọi trường dữ liệu nhận từ Client phải có chú thích validation tương ứng:
  - Chuỗi văn bản: `@NotBlank(message = "...")`
  - Định dạng email: `@Email(message = "...")`
  - Độ dài mật khẩu / chuỗi: `@Size(min = 8, max = 100, message = "...")`
  - Biểu thức chính quy (SĐT, Password mạnh): `@Pattern(regexp = "...", message = "...")`
  - Số lượng, giá trị: `@Min`, `@Max`, `@Positive`
- Tại Controller, request body bắt buộc phải có annotation `@Valid`:
  ```java
  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) { ... }
  ```

---

## 5. Chuẩn 5: Chuẩn hóa phản hồi API & Xử lý ngoại lệ (Envelope & Exception Handling)

### 5.1. Định dạng JSON Envelope thống nhất (`ApiResponse<T>`)
Mọi API trả về cho Client đều phải tuân thủ chuẩn vỏ bọc thống nhất `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Cập nhật hồ sơ thành công",
  "data": { ... },
  "timestamp": "2026-09-16T18:00:00.000000"
}
```

Nếu là danh sách có phân trang, sử dụng cấu trúc `PageResponse<T>` trong trường `data`:
```json
{
  "success": true,
  "message": "Lấy danh sách thành công",
  "data": {
    "items": [ ... ],
    "page": 0,
    "size": 10,
    "totalElements": 150,
    "totalPages": 15,
    "isLast": false
  },
  "timestamp": "2026-09-16T18:00:00.000000"
}
```

### 5.2. Quản lý lỗi tập trung (`GlobalExceptionHandler`)
- Mọi ngoại lệ nghiệp vụ phải kế thừa từ `RuntimeException` và có class Exception riêng biệt:
  - `BadRequestException` $\rightarrow$ Trả về HTTP 400
  - `UnauthorizedException` $\rightarrow$ Trả về HTTP 401
  - `ForbiddenException` $\rightarrow$ Trả về HTTP 403
  - `NotFoundException` $\rightarrow$ Trả về HTTP 404
  - `ConflictException` $\rightarrow$ Trả về HTTP 409
- Định dạng JSON khi gặp lỗi:
  ```json
  {
    "success": false,
    "message": "Email đã được sử dụng",
    "path": "/api/v1/auth/register",
    "errors": [],
    "timestamp": "2026-09-16T18:00:00.000000"
  }
  ```
- **Tuyệt đối không rò rỉ Stacktrace** của Java ra ngoài response của Client trên môi trường Production.

---

## 6. Chuẩn 6: Độc lập CSDL & Quản lý Migration (Database-per-Service & Flyway)

### 6.1. Nguyên tắc Database-per-Service
- Mỗi microservice sở hữu CSDL riêng biệt (Schema riêng hoặc Instance riêng).
- **NGHIÊM CẤM**:
  - Không truy vấn chéo (cross-database query) vào bảng của service khác.
  - Không sử dụng ràng buộc khóa ngoại (Foreign Key) trỏ sang bảng của service khác.
- **Cách thức liên kết**:
  - Chỉ lưu trữ mã định danh duy nhất (ví dụ: `Long userId`, `Long cinemaId`, `Long movieId`, `Long bookingId`).
  - Khi cần dữ liệu chi tiết, gọi API nội bộ qua REST Client (`WebClient` / `FeignClient`) hoặc lắng nghe Message Broker (RabbitMQ/Kafka).

### 6.2. Quản lý CSDL bằng Flyway Migration
- Mọi thao tác tạo bảng, thêm cột, chỉnh sửa kiểu dữ liệu, đánh index đều phải viết thành file SQL trong `src/main/resources/db/migration/`.
- Tên file tuân theo chuẩn: `V<version>__<description>.sql` (Ví dụ: `V1__init_identity.sql`, `V2__add_avatar_column.sql`).
- Cấu hình JPA:
  ```properties
  spring.jpa.hibernate.ddl-auto=validate
  spring.flyway.enabled=true
  spring.flyway.baseline-on-migrate=true
  ```
- Không được dùng `ddl-auto=create` hoặc `ddl-auto=update` trên môi trường Production.

---

## 7. Chuẩn 7: Truy vết phân tán & Ghi log (Distributed Tracing & Correlation ID)

### 7.1. Chuẩn Correlation ID (`X-Correlation-Id`)
- API Gateway sẽ tự động kiểm tra:
  - Nếu request gửi lên đã có header `X-Correlation-Id`, Gateway giữ nguyên.
  - Nếu chưa có, Gateway tự động sinh một mã ngẫu nhiên UUID: `UUID.randomUUID().toString()`.
- Gateway chuyển tiếp header `X-Correlation-Id` này tới tất cả microservice liên quan trong chuỗi xử lý.
- Microservice nhận được header sẽ ghi `correlationId` vào **MDC (Mapped Diagnostic Context)** của SLF4J.
- Mọi log ghi ra console/file đều có kèm mã `[correlationId]` để dễ dàng tra cứu toàn bộ luồng xử lý từ Gateway đến Database.

### 7.2. Chuẩn ghi Log (Logging Standards)
- Dùng thư viện chuẩn `org.slf4j.Logger` (thông qua Lombok `@Slf4j`).
- **Phân định cấp độ Log**:
  - `INFO`: Ghi nhận các sự kiện vòng đời nghiệp vụ chính (User login, Order placed, Payment success).
  - `WARN`: Các tình huống bất thường nhưng ứng dụng tự hồi phục được (Retry kết nối, Cache miss, OTP sai lần 1).
  - `ERROR`: Các lỗi ngoại lệ gây gián đoạn luồng xử lý (Database down, SMTP connection error, Bug 500).
- **Tuyệt đối cấm**: Không sử dụng `System.out.println()` hoặc `e.printStackTrace()` trong mã nguồn nghiệp vụ.

---

## 8. Chuẩn 8: Quy ước đặt tên & Thiết kế RESTful URI

### 8.1. Quy ước đặt URI API
- Sử dụng **danh từ số nhiều**, chữ thường (lowercase), phân cách từ ghép bằng dấu gạch ngang (kebab-case).
- Mọi URI phải có tiền tố phiên bản: `/api/v1/`.
- Phân tầng phân quyền rõ ràng:
  - Public & Khách hàng: `/api/v1/<tên_tài_nguyên>` (Ví dụ: `/api/v1/movies`, `/api/v1/users/me`)
  - Nghiệp vụ Nhân viên: `/api/v1/staff/<tên_tài_nguyên>` (Ví dụ: `/api/v1/staff/check-in`)
  - Quản trị viên: `/api/v1/admin/<tên_tài_nguyên>` (Ví dụ: `/api/v1/admin/users`, `/api/v1/admin/movies`)

### 8.2. Quy ước sử dụng HTTP Method
- `GET`: Truy vấn dữ liệu (Idempotent, không thay đổi trạng thái hệ thống).
- `POST`: Tạo mới tài nguyên hoặc kích hoạt hành vi nghiệp vụ đặc thù (Register, Login, Checkout).
- `PUT`: Cập nhật toàn phần hoặc thay thế tài nguyên.
- `PATCH`: Cập nhật từng phần (Partial update).
- `DELETE`: Xóa tài nguyên hoặc hủy kích hoạt (Soft delete).

### 8.3. Mã trạng thái HTTP chuẩn (HTTP Status Codes)
- `200 OK`: Thành công cho `GET`, `PUT`, `PATCH`.
- `201 Created`: Tạo mới thành công cho `POST`.
- `204 No Content`: Xóa thành công hoặc không có dữ liệu trả về cho `DELETE`.
- `400 Bad Request`: Dữ liệu đầu vào sai định dạng hoặc vi phạm validation.
- `401 Unauthorized`: Chưa đăng nhập hoặc JWT Token không hợp lệ / hết hạn.
- `403 Forbidden`: Đã đăng nhập nhưng không đủ quyền hạn truy cập (Role không khớp).
- `404 Not Found`: Không tìm thấy tài nguyên theo định danh được yêu cầu.
- `409 Conflict`: Xung đột trạng thái dữ liệu (Email đã tồn tại, Ghế đã có người đặt).
- `500 Internal Server Error`: Lỗi hệ thống ngoài tầm kiểm soát.

---

## BẢNG TỔNG KẾT TUÂN THỦ (CHECKLIST)

| Hạng mục kiểm tra | Tiêu chuẩn bắt buộc |
| :--- | :--- |
| **application.properties** | Chỉ dùng `${VARIABLE_NAME}`, **không có giá trị mặc định sau dấu `:`** |
| **Biến môi trường (.env)** | Khai báo 100% biến trong `.env`, có `.env.example`, thêm `.env` vào `.gitignore` |
| **Cổng truy cập** | Toàn bộ request từ bên ngoài phải đi qua Gateway cổng **`8080`** |
| **Bảo mật Port** | Các port con (`8081`, `8082`,...) không mở public; chặn bằng network + `X-Gateway-Secret` |
| **Tầng Service** | 100% Service phải có `Interface` và `ServiceImpl`; inject qua Interface |
| **DTO** | 100% DTO dùng `record` + Jakarta Validation (`@Valid`, `@NotBlank`,...) |
| **API Envelope** | 100% API bọc trong `ApiResponse<T>`, danh sách dùng `PageResponse<T>` |
| **Database** | Database riêng cho từng service; liên kết qua `ID`; dùng Flyway migration |
| **Swagger / OpenAPI** | Cung cấp tài liệu OpenAPI, Bearer JWT Auth và aggregate về Gateway |
