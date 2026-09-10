# Phase 2 - Post-response Scripts

Dán phần **Post-response** vào tab **Scripts -> Post-response** trong Postman.

## 1. Đăng ký

### API
```http
POST /api/v1/auth/register
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Đăng ký thành công", function () {
  pm.expect(pm.response.code).to.eql(201);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.user.roles).to.include("CUSTOMER");
  pm.expect(body.data.emailVerificationRequired).to.eql(true);
});

pm.collectionVariables.set("customerEmail", body.data.user.email);
pm.collectionVariables.set("customerBirthYear", body.data.user.birthYear);
pm.collectionVariables.set("emailVerificationRequired", body.data.emailVerificationRequired);
```

## 2. Gửi lại OTP xác minh email

### API
```http
POST /api/v1/auth/verify-email/request
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Gửi lại OTP xác minh email thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});
```

## 3. Xác minh email

### API
```http
POST /api/v1/auth/verify-email
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Xác minh email thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("emailVerified", true);
```

## 4. Đăng nhập customer

### API
```http
POST /api/v1/auth/login
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Đăng nhập customer thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.tokenType).to.eql("Bearer");
  pm.expect(body.data.accessToken).to.not.be.empty;
  pm.expect(body.data.refreshToken).to.not.be.empty;
});

pm.collectionVariables.set("accessToken", body.data.accessToken);
pm.collectionVariables.set("refreshToken", body.data.refreshToken);
pm.collectionVariables.set("tokenType", body.data.tokenType || "Bearer");

if (body.data.user?.id) {
  pm.collectionVariables.set("userId", body.data.user.id);
}

if (body.data.user?.email) {
  pm.collectionVariables.set("userEmail", body.data.user.email);
}
```

## 5. Đăng nhập admin seed

### API
```http
POST /api/v1/auth/login
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Đăng nhập admin seed thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.accessToken).to.not.be.empty;
  pm.expect(body.data.roles).to.include("ADMIN");
});

pm.collectionVariables.set("adminToken", body.data.accessToken);
pm.collectionVariables.set("adminRefreshToken", body.data.refreshToken);

if (body.data.user?.id) {
  pm.collectionVariables.set("adminUserId", body.data.user.id);
}

if (body.data.user?.email) {
  pm.collectionVariables.set("adminEmail", body.data.user.email);
}
```

## 6. Lấy user hiện tại

### API
```http
GET /api/v1/users/me
Authorization: Bearer {{accessToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Lấy user hiện tại thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.email).to.not.be.empty;
  pm.expect(body.data.roles).to.be.an("array");
});

pm.collectionVariables.set("userId", body.data.id);
pm.collectionVariables.set("userEmail", body.data.email);
pm.collectionVariables.set("userStatus", body.data.status);
```

## 7. Cập nhật profile

### API
```http
PUT /api/v1/users/me
Authorization: Bearer {{accessToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Cập nhật profile thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.fullName).to.not.be.empty;
});

pm.collectionVariables.set("userFullName", body.data.fullName);
pm.collectionVariables.set("userPhone", body.data.phone);
pm.collectionVariables.set("userBirthYear", body.data.birthYear);
```

## 8. Đổi mật khẩu

### API
```http
POST /api/v1/users/me/password
Authorization: Bearer {{accessToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Đổi mật khẩu thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});
```

## 9. Refresh token

### API
```http
POST /api/v1/auth/refresh
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Refresh token thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.accessToken).to.not.be.empty;
});

pm.collectionVariables.set("accessToken", body.data.accessToken);

if (body.data.refreshToken) {
  pm.collectionVariables.set("refreshToken", body.data.refreshToken);
}
```

## 10. Đăng xuất

### API
```http
POST /api/v1/auth/logout
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Đăng xuất thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.unset("accessToken");
pm.collectionVariables.unset("refreshToken");
```

## 11. Quên mật khẩu - gửi OTP

### API
```http
POST /api/v1/auth/password-reset/request
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Gửi OTP reset password thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.token).to.match(/^[0-9]{6}$/);
});

pm.collectionVariables.set("passwordResetOtp", body.data.token);
```

## 12. Quên mật khẩu - xác nhận OTP

### API
```http
POST /api/v1/auth/password-reset/confirm
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Reset password thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});
```

## 13. Google login

### API
```http
POST /api/v1/auth/google
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Google login trả response hợp lệ", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 400, 401]);
});

if (pm.response.code === 200) {
  pm.collectionVariables.set("accessToken", body.data.accessToken);
  pm.collectionVariables.set("refreshToken", body.data.refreshToken);
}
```

## 14. Google OTP verify

### API
```http
POST /api/v1/auth/google/verify
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Google OTP verify trả response hợp lệ", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 400, 401]);
});

if (pm.response.code === 200) {
  pm.collectionVariables.set("accessToken", body.data.accessToken);
  pm.collectionVariables.set("refreshToken", body.data.refreshToken);
}
```

## 15. Admin - danh sách user

### API
```http
GET /api/v1/admin/users
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Admin xem danh sách user thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data).to.be.an("array");
});
```

## 16. Admin - tạo staff

### API
```http
POST /api/v1/admin/users/staff
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Admin tạo staff thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.roles).to.include("STAFF");
});

pm.collectionVariables.set("staffUserId", body.data.id);
pm.collectionVariables.set("staffEmail", body.data.email);
```

## 17. Admin - đổi trạng thái user

### API
```http
PATCH /api/v1/admin/users/{{userId}}/status
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Admin đổi trạng thái user thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.not.be.empty;
});

pm.collectionVariables.set("userStatus", body.data.status);
```
