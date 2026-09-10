# CinemaAI Architecture

## 1. Tổng quan

CinemaAI là Spring Boot monolith theo package-layered architecture. Code đã tách controller, service interface/implementation, repository, entity, DTO và mapper, nên có hướng Clean Architecture; tuy nhiên chưa phải Clean Architecture thuần vì service phụ thuộc trực tiếp Spring Data/JPA entity và toàn bộ layer nằm trong một Maven module.

```text
HTTP / JWT
    |
controller + validation
    |
service interfaces (use-case boundary)
    |
service/impl (business rules, transactions, orchestration)
    |----------------------|
repository/JPA             external adapters
    |                      VNPay, Google, SMTP, Cloudinary
entity + PostgreSQL
```

## 2. Layers hiện tại

| Layer/package | Trách nhiệm | Được phụ thuộc |
|---|---|---|
| `controller` | REST mapping, request/query/path, principal, status code | DTO, service interface, security principal |
| `dto.request` | Input contract và validation | enum, Jakarta Validation |
| `dto.response` | Output contract | enum/primitive/nested response |
| `service` | Interface cho use case | DTO/domain type cần thiết |
| `service.impl` | Business rule, transaction, state transition, provider orchestration | service, repository, mapper, entity, config |
| `mapper` | Entity-to-response mapping | entity, response DTO |
| `repository` | Spring Data CRUD và query | entity, projection |
| `entity`, `enums` | Persistence model và vocabulary nghiệp vụ | JPA/Lombok; không phụ thuộc web |
| `security` | JWT, principal, user details, authorization config | service/repository khi xác thực |
| `config` | Bean/properties/integration configuration | framework/provider |
| `scheduler` | Tác vụ nền dọn hold và cập nhật showtime | service/repository |
| `exception` | Domain/application HTTP exception mapping | response DTO |
| `recommendation` | Strategy và scoring recommendation | domain/service data |
| `seeder` | Dữ liệu bootstrap local | repository/entity |
| `db/migration` | Schema version scripts; cần dùng PostgreSQL dialect | PostgreSQL |

## 3. Dependency rule

Rule áp dụng cho thay đổi mới:

```text
web/framework details -> application use cases -> domain concepts
database/provider details ----------------------^
```

Trong cấu trúc hiện tại:

- Controller không gọi repository.
- Controller không tự chuyển trạng thái booking/payment/showtime.
- Repository không gọi service hoặc controller.
- Entity không import DTO/controller/security web.
- Mapper không query database.
- Service implementation là transaction boundary mặc định.
- Provider callback phải được xác minh trong adapter/service trước khi đổi state.

Nếu refactor theo Clean Architecture mạnh hơn, ưu tiên dần:

1. Tách domain model khỏi JPA entity.
2. Định nghĩa repository/provider ports ở application layer.
3. Đưa Spring Data, VNPay, Cloudinary, SMTP thành outbound adapters.
4. Tách module Maven theo `domain`, `application`, `infrastructure`, `bootstrap`.

Không thực hiện migration kiến trúc lớn trong feature task thông thường.

## 4. Request lifecycle

```text
Request
 -> CorrelationIdFilter / RequestLoggingFilter
 -> JwtAuthenticationFilter
 -> SecurityConfig + @PreAuthorize
 -> Controller + @Valid
 -> Service interface
 -> @Transactional service implementation
 -> Repository / provider
 -> Mapper + ApiResponse
 -> GlobalExceptionHandler khi có lỗi
```

Success envelope:

```json
{
  "success": true,
  "data": {},
  "message": "Success",
  "timestamp": "2026-06-25T12:00:00"
}
```

Error envelope:

```json
{
  "success": false,
  "message": "Validation failed",
  "path": "/api/v1/example",
  "errors": [{"field": "name", "message": "must not be blank"}],
  "timestamp": "2026-06-25T12:00:00"
}
```

## 5. Security boundary

- Stateless JWT; CSRF disabled.
- Public: auth, actuator/Swagger, catalog GET, VNPay return/IPN.
- `/api/v1/admin/**`: role `ADMIN`.
- `/api/v1/staff/**`: role `ADMIN` hoặc `STAFF`.
- Các endpoint còn lại cần authenticated user; một số review/notification có `@PreAuthorize`.
- Password dùng BCrypt strength 10.

Lưu ý: `StaffController` có alias `/api/v1/admin/check-in`; do admin path được SecurityConfig cho ADMIN và staff path cho ADMIN/STAFF.

## 6. Persistence và migration

- Local/production runtime mục tiêu: PostgreSQL, `ddl-auto=update`.
- Test: H2 in-memory, `ddl-auto=create-drop`.
- Migration `V1` đến `V5` nằm tại `src/main/resources/db/migration`.
- Flyway hiện bị tắt và `pom.xml` chưa khai báo `flyway-core`; vì vậy migration chưa là executable source of truth.
- Các migration hiện còn dùng cú pháp SQL Server như `dbo`, `IDENTITY`, `NVARCHAR`, `DATETIME2`, `IF OBJECT_ID`; cần chuyển sang PostgreSQL trước khi bật Flyway.
- Có logic startup `TicketPricingSchemaCleanup` dùng `database()` theo MySQL; logic này không tương thích PostgreSQL và H2.

Mục tiêu kiến trúc:

- Production: Flyway enabled, Hibernate `validate`.
- Test migration: chạy migration trên PostgreSQL/Testcontainers hoặc một PostgreSQL disposable database.
- Không đồng thời coi `ddl-auto=update` và migration SQL là hai nguồn schema ngang nhau.
- Loại bỏ MySQL connector sau khi xác nhận không còn profile nào sử dụng; PostgreSQL driver là runtime driver chính.

## 7. External integrations

| Integration | Adapter/config |
|---|---|
| VNPay | `VNPayService`, `PaymentService`, `VNPayConfig`, `VnpayProperties` |
| Google login | `GoogleTokenVerifier`, `GoogleAuthProperties` |
| Email/OTP | `MailService`, Spring Mail |
| Cloudinary | `CloudinaryUploadService`, `CloudinaryConfig` |
| Recommendation | strategy classes + configurable signal weights |

Không gọi SDK/HTTP provider trực tiếp từ controller.

## 8. Cross-cutting concerns

- Audit timestamps: `BaseEntity`, JPA auditing.
- Error translation: `GlobalExceptionHandler`.
- Correlation/logging: web filters.
- Async: `AsyncConfig`.
- Background jobs: seat hold cleanup và showtime status scheduler.
- API discovery: Springdoc OpenAPI/Swagger.

## 9. Testing architecture

- Integration tests dùng Spring context + H2.
- Inventory tests đọc source file để xác nhận endpoint/package.
- Postman collection mô tả flow Phase 0–6.
- Baseline 2026-06-25: build pass, full tests fail do inventory test stale và schema cleanup mang cú pháp MySQL chạy trên H2. Cùng logic đó cũng không phù hợp PostgreSQL.
