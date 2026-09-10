# Phase 0 - Post-response Scripts

Dán phần **Post-response** vào tab **Scripts -> Post-response** trong Postman.

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
```javascript
pm.test("Swagger UI trả HTML hoặc redirect hợp lệ", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 301, 302]);
});

pm.test("Correlation id được trả về", function () {
  pm.expect(pm.response.headers.has("X-Correlation-Id")).to.eql(true);
});
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
```javascript
const body = pm.response.json();
const bearer = body.components?.securitySchemes?.["Bearer Authentication"];

pm.test("OpenAPI docs trả về thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body).to.have.property("openapi");
  pm.expect(body).to.have.property("paths");
});

pm.test("OpenAPI info đúng CineAI", function () {
  pm.expect(body.info?.title).to.eql("CineAI API");
  pm.expect(body.info?.version).to.eql("v1");
});

pm.test("OpenAPI có Bearer JWT security scheme", function () {
  pm.expect(bearer?.type).to.eql("http");
  pm.expect(bearer?.scheme).to.eql("bearer");
  pm.expect(bearer?.bearerFormat).to.eql("JWT");
});

pm.test("Correlation id được trả về", function () {
  pm.expect(pm.response.headers.has("X-Correlation-Id")).to.eql(true);
});

if (body.info?.title) {
  pm.collectionVariables.set("openapiTitle", body.info.title);
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
```javascript
const body = pm.response.json();

pm.test("Health check thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.status).to.eql("UP");
});

pm.test("Correlation id được trả về", function () {
  pm.expect(pm.response.headers.has("X-Correlation-Id")).to.eql(true);
});
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
```javascript
pm.test("Correlation id được echo lại đúng giá trị", function () {
  pm.expect(pm.response.headers.get("X-Correlation-Id")).to.eql("phase-0-correlation-id");
});
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
```javascript
let body = {};

try {
  body = pm.response.json();
} catch (error) {
  console.log("Response không phải JSON");
}

pm.test("Endpoint cần đăng nhập trả Unauthorized/Forbidden", function () {
  pm.expect(pm.response.code).to.be.oneOf([401, 403]);
});

pm.test("Correlation id được trả về", function () {
  pm.expect(pm.response.headers.has("X-Correlation-Id")).to.eql(true);
});

if (body.message) {
  pm.collectionVariables.set("lastAuthError", body.message);
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
```javascript
const body = pm.response.json();

pm.test("Endpoint không tồn tại trả 404", function () {
  pm.expect(pm.response.code).to.eql(404);
});

pm.test("Error response đúng contract chung", function () {
  pm.expect(body.success).to.eql(false);
  pm.expect(body).to.have.property("message");
  pm.expect(body.path).to.eql("/api/v1/auth/phase-0-not-found");
  pm.expect(body.errors).to.be.an("array");
  pm.expect(body).to.have.property("timestamp");
});

pm.test("Correlation id được trả về", function () {
  pm.expect(pm.response.headers.has("X-Correlation-Id")).to.eql(true);
});
```
