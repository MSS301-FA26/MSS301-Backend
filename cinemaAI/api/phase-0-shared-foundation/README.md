# Phase 0 - Nền tảng dùng chung

Phase này kiểm tra các thành phần nền tảng được dùng xuyên suốt hệ thống:
REST API contract, Swagger/OpenAPI, health check, correlation id, response
wrapper, error wrapper và lớp bảo mật JWT/RBAC.

## Quy ước chung

### Base URL
```text
{{baseUrl}}
```

Ví dụ local:
```text
http://localhost:8080
```

### Header khuyến nghị
```http
Content-Type: application/json
X-Correlation-Id: phase-0-manual-test
```

Nếu client không gửi `X-Correlation-Id`, backend tự sinh UUID và trả lại trong
response header.

### Success response chuẩn
```json
{
  "success": true,
  "data": {},
  "message": "Thành công",
  "timestamp": "2026-06-13T20:00:00"
}
```

### Error response chuẩn
```json
{
  "success": false,
  "message": "Unauthorized",
  "path": "/api/v1/users/me",
  "errors": [],
  "timestamp": "2026-06-13T20:00:00"
}
```

## 1. Swagger UI

### Tên mô tả API
Mở Swagger UI để xem và test toàn bộ API contract.

### API
```http
GET /swagger-ui/index.html
```

### JSON
```json
{}
```

### Post-response
```text
Trang HTML Swagger UI hoặc redirect hợp lệ.
```

## 2. OpenAPI Docs

### Tên mô tả API
Lấy tài liệu OpenAPI dạng JSON.

### API
```http
GET /v3/api-docs
```

### JSON
```json
{}
```

### Post-response
```json
{
  "openapi": "3.1.0",
  "info": {
    "title": "CineAI API",
    "version": "v1"
  },
  "components": {
    "securitySchemes": {
      "Bearer Authentication": {
        "type": "http",
        "scheme": "bearer",
        "bearerFormat": "JWT"
      }
    }
  }
}
```

## 3. Health Check

### Tên mô tả API
Kiểm tra ứng dụng đang chạy và các dependency nền tảng còn sẵn sàng.

### API
```http
GET /actuator/health
```

### JSON
```json
{}
```

### Post-response
```json
{
  "status": "UP"
}
```

## 4. Correlation Id

### Tên mô tả API
Kiểm tra backend echo lại `X-Correlation-Id` để trace request xuyên suốt log.

### API
```http
GET /v3/api-docs
X-Correlation-Id: phase-0-correlation-id
```

### JSON
```json
{}
```

### Post-response
```text
Response header X-Correlation-Id = phase-0-correlation-id.
```

## 5. Kiểm tra endpoint cần đăng nhập

### Tên mô tả API
Kiểm tra security foundation. Nếu gọi endpoint cần token mà không gửi token thì
backend trả lỗi xác thực.

### API
```http
GET /api/v1/users/me
```

### JSON
```json
{}
```

### Post-response
```json
{
  "success": false,
  "message": "Unauthorized",
  "path": "/api/v1/users/me",
  "errors": [],
  "timestamp": "2026-06-13T20:00:00"
}
```

## 6. Kiểm tra lỗi 404 chuẩn

### Tên mô tả API
Kiểm tra endpoint không tồn tại trả về error wrapper thống nhất.

### API
```http
GET /api/v1/auth/phase-0-not-found
```

### JSON
```json
{}
```

### Post-response
```json
{
  "success": false,
  "message": "Không tìm thấy tài nguyên",
  "path": "/api/v1/auth/phase-0-not-found",
  "errors": [],
  "timestamp": "2026-06-13T20:00:00"
}
```

## Checklist hoàn tất Phase 0

- Swagger UI truy cập được.
- OpenAPI docs có title, version và Bearer JWT security scheme.
- Actuator health trả `UP`.
- Response luôn có `X-Correlation-Id`.
- Endpoint protected trả `401` hoặc `403` khi thiếu token.
- Error response dùng chung cấu trúc `success/message/path/errors/timestamp`.
- Security chạy stateless, dùng JWT filter, phân quyền `/admin/**` và `/staff/**`.
