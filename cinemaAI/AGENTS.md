# CinemaAI Agent Working Agreement

File này là điểm vào bắt buộc cho mọi coding agent làm việc trong repository.

## 1. Startup checklist

Trước khi sửa code:

1. Xác nhận repository root có `pom.xml`, `mvnw`, `mvnw.cmd`.
2. Đọc `progress.md` và `feature_list.json`.
3. Đọc tài liệu theo phạm vi task:
   - kiến trúc: `architecture.md`
   - API: `api-contract.md`
   - nghiệp vụ: `domain-rules.md`
4. Chạy `git status --short`; không ghi đè thay đổi chưa commit của người dùng.
5. Chọn một feature/task có scope rõ ràng; nêu file dự kiến sửa và cách verify.

## 2. Stack và lệnh chuẩn

- Java 17 source level; môi trường hiện tại có thể dùng JDK 21.
- Spring Boot 3.5.x, Maven Wrapper, Spring Web, Security, Data JPA.
- Production/local database: PostgreSQL.
- Test database: H2 in-memory.

PowerShell:

```powershell
.\init.ps1
.\mvnw.cmd -DskipTests package
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw test
./mvnw spring-boot:run
```

Swagger: `http://localhost:8080/swagger-ui.html`.

## 3. Scope control

- Chỉ sửa file liên quan trực tiếp đến task.
- Không refactor toàn repository trong một bug fix hoặc endpoint nhỏ.
- Không đổi public API, enum, database schema, authentication/authorization hoặc state machine nếu task không yêu cầu.
- Không sửa migration cũ đã được áp dụng; tạo migration version mới.
- Không xóa file, force-push, reset hard, sửa secret thật hoặc deploy production nếu chưa được người dùng cho phép.
- `.env` là dữ liệu local; chỉ cập nhật `.env.example` bằng placeholder.

## 4. Architecture rules

Luồng phụ thuộc mong muốn:

```text
controller -> service interface -> service implementation -> repository -> entity/database
                    |                       |
                    +---- DTO/mapper -------+
```

- Controller chỉ nhận/validate input, lấy principal, gọi use case và đóng gói response.
- Business rule, transaction và state transition nằm trong service implementation.
- Repository chỉ chứa persistence/query; không chứa orchestration nghiệp vụ.
- Entity không phụ thuộc controller hoặc DTO.
- Request/response không trả entity JPA trực tiếp; dùng DTO và mapper.
- External providers (VNPay, Cloudinary, Google, SMTP) phải đi qua service/config adapter.
- Chi tiết và giới hạn Clean Architecture hiện tại nằm trong `architecture.md`.

## 5. API rules

- Base path: `/api/v1`.
- Success response mặc định dùng `ApiResponse<T>`.
- Error response dùng `ErrorResponse`.
- Dùng `@Valid` cho request body và Jakarta Validation trong request DTO.
- Endpoint tạo resource trả `201`; delete thật sự không có body trả `204`; còn lại mặc định `200`.
- Giữ RBAC đồng bộ giữa `SecurityConfig`, `@PreAuthorize`, Swagger và test.
- Khi thêm/sửa endpoint, cập nhật `api-contract.md` và test liên quan.

## 6. Domain invariants

- Không bypass các state machine của booking, payment, showtime, seat runtime và review.
- Hold ghế hiện tại kéo dài 10 phút.
- Ghế `HOLDING`, `BOOKED`, `CHECKED_IN` chặn hold mới.
- Chỉ booking `PAID` được check-in; check-in chuyển thành `USED`.
- Chỉ user đã xem phim (`USED`) mới được review và mỗi user chỉ review một lần/phim.
- Hệ thống hiện giới hạn một cinema.
- Xem đầy đủ tại `domain-rules.md`.

## 7. Verification và Definition of Done

Mức verify tối thiểu:

1. Build: `.\mvnw.cmd -DskipTests package`.
2. Test hẹp cho module vừa sửa.
3. Full test: `.\mvnw.cmd test` nếu thời gian cho phép.
4. Với API: kiểm tra mapping, auth/role, request validation, success/error status.
5. Với DB: kiểm tra migration theo thứ tự và trên database disposable trước.

Chỉ đánh dấu `done` khi có evidence. Nếu test fail do baseline:

- không che giấu hoặc sửa unrelated chỉ để xanh;
- ghi rõ lỗi baseline và test hẹp đã chạy;
- cập nhật `progress.md` và `feature_list.json`.

## 8. State handoff

Cuối mỗi phiên có thay đổi:

- cập nhật `progress.md`: đã làm, file đổi, lệnh verify, lỗi còn lại, next action;
- cập nhật đúng feature trong `feature_list.json`;
- trạng thái chỉ dùng `done`, `pending`, `blocked`;
- không đánh dấu `done` nếu verification bắt buộc chưa pass.

## 9. Known baseline on 2026-06-25

- `.\mvnw.cmd -DskipTests package`: pass.
- `.\mvnw.cmd test`: fail.
- Inventory tests còn tham chiếu các controller cũ trước khi hợp nhất.
- `TicketPricingSchemaCleanup` còn dùng truy vấn metadata đặc thù MySQL (`database()`), không tương thích PostgreSQL/H2 và thao tác enum `SENIOR`, gây lỗi test.
- Flyway scripts có trong source nhưng runtime chưa có `flyway-core` và `spring.flyway.enabled=false`.
- Các migration hiện có còn cú pháp SQL Server (`dbo`, `IDENTITY`, `NVARCHAR`, `DATETIME2`), chưa thể chạy trực tiếp trên PostgreSQL.
