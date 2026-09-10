# Phase 2 - Auth, user và security

Phase 2 bao phủ đăng ký, xác minh email OTP, đăng nhập email/password, Google
login, refresh token, logout, profile cá nhân, đổi mật khẩu và admin quản lý
user/staff. Các endpoint protected dùng Bearer JWT.

## 1. Đăng ký

### Tên mô tả API
Đăng ký tài khoản customer. Backend lưu tạm vào `pending_registrations` và gửi
OTP email.

### API
```http
POST /api/v1/auth/register
```

### JSON
```json
{
  "email": "customer@example.com",
  "password": "{{customerPassword}}",
  "fullName": "Customer One",
  "phone": "0900111222",
  "birthYear": 2000
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "user": {
      "email": "customer@example.com",
      "fullName": "Customer One",
      "phone": "0900111222",
      "birthYear": 2000,
      "roles": ["CUSTOMER"],
      "emailVerified": false
    },
    "emailVerificationRequired": true,
    "emailVerificationExpiresInSeconds": 300
  },
  "message": "Registered successfully"
}
```

## 2. Gửi lại OTP xác minh email

### Tên mô tả API
Gửi lại OTP cho email đang chờ xác minh.

### API
```http
POST /api/v1/auth/verify-email/request
```

### JSON
```json
{
  "email": "customer@example.com"
}
```

### Post-response
```json
{
  "success": true,
  "data": null,
  "message": "Email verification OTP sent"
}
```

## 3. Xác minh email

### Tên mô tả API
Xác minh OTP email để tạo user thật.

### API
```http
POST /api/v1/auth/verify-email
```

### JSON
```json
{
  "email": "customer@example.com",
  "otp": "123456"
}
```

### Post-response
```json
{
  "success": true,
  "data": null,
  "message": "Email verified successfully"
}
```

## 4. Đăng nhập

### Tên mô tả API
Đăng nhập bằng email/password và nhận access token, refresh token.

### API
```http
POST /api/v1/auth/login
```

### JSON
```json
{
  "username": "customer@example.com",
  "password": "{{customerPassword}}"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "accessToken": "JWT_ACCESS_TOKEN",
    "refreshToken": "REFRESH_TOKEN",
    "tokenType": "Bearer",
    "expiresInMs": 900000,
    "user": {
      "id": 1,
      "email": "customer@example.com",
      "roles": ["CUSTOMER"],
      "emailVerified": true
    },
    "roles": ["CUSTOMER"]
  },
  "message": "Logged in successfully"
}
```

## 5. Đăng nhập admin seed

### Tên mô tả API
Đăng nhập bằng tài khoản admin seed để test API admin.

### API
```http
POST /api/v1/auth/login
```

### JSON
```json
{
  "username": "admin@cinemaai.com",
  "password": "{{adminPassword}}"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "accessToken": "ADMIN_JWT_ACCESS_TOKEN",
    "refreshToken": "ADMIN_REFRESH_TOKEN",
    "tokenType": "Bearer",
    "roles": ["ADMIN"]
  },
  "message": "Logged in successfully"
}
```

## 6. Lấy thông tin user hiện tại

### Tên mô tả API
Lấy profile user đang đăng nhập.

### API
```http
GET /api/v1/users/me
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "fullName": "Customer One",
    "phone": "0900111222",
    "birthYear": 2000,
    "status": "ACTIVE",
    "emailVerified": true,
    "phoneVerified": false,
    "roles": ["CUSTOMER"]
  }
}
```

## 7. Cập nhật profile

### Tên mô tả API
Cập nhật họ tên, số điện thoại và năm sinh của user hiện tại.

### API
```http
PUT /api/v1/users/me
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "fullName": "Customer Updated",
  "phone": "0900111333",
  "birthYear": 1998
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "fullName": "Customer Updated",
    "phone": "0900111333",
    "birthYear": 1998
  },
  "message": "Profile updated successfully"
}
```

## 8. Đổi mật khẩu

### Tên mô tả API
Đổi mật khẩu user hiện tại.

### API
```http
POST /api/v1/users/me/password
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "oldPassword": "{{customerPassword}}",
  "newPassword": "NewPassword456",
  "confirmPassword": "NewPassword456"
}
```

### Post-response
```json
{
  "success": true,
  "data": null,
  "message": "Password changed successfully"
}
```

## 9. Refresh token

### Tên mô tả API
Cấp access token mới từ refresh token.

### API
```http
POST /api/v1/auth/refresh
```

### JSON
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "accessToken": "NEW_JWT_ACCESS_TOKEN",
    "refreshToken": "REFRESH_TOKEN",
    "tokenType": "Bearer"
  },
  "message": "Token refreshed successfully"
}
```

## 10. Đăng xuất

### Tên mô tả API
Thu hồi refresh token.

### API
```http
POST /api/v1/auth/logout
```

### JSON
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

### Post-response
```json
{
  "success": true,
  "data": null,
  "message": "Logged out successfully"
}
```

## 11. Quên mật khẩu - gửi OTP

### Tên mô tả API
Gửi OTP reset password về email user.

### API
```http
POST /api/v1/auth/password-reset/request
```

### JSON
```json
{
  "email": "customer@example.com"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "token": "123456"
  },
  "message": "Password reset OTP sent"
}
```

## 12. Quên mật khẩu - xác nhận OTP

### Tên mô tả API
Xác nhận OTP và đặt mật khẩu mới.

### API
```http
POST /api/v1/auth/password-reset/confirm
```

### JSON
```json
{
  "email": "customer@example.com",
  "otp": "123456",
  "newPassword": "ResetPassword789",
  "confirmPassword": "ResetPassword789"
}
```

### Post-response
```json
{
  "success": true,
  "data": null,
  "message": "Password reset successfully"
}
```

## 13. Google login

### Tên mô tả API
Đăng nhập bằng Google credential. Nếu cần OTP email, dùng bước Google verify.

### API
```http
POST /api/v1/auth/google
```

### JSON
```json
{
  "credential": "GOOGLE_ID_TOKEN"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "accessToken": "JWT_ACCESS_TOKEN",
    "refreshToken": "REFRESH_TOKEN",
    "tokenType": "Bearer"
  },
  "message": "Logged in with Google successfully"
}
```

## 14. Google OTP verify

### Tên mô tả API
Xác minh OTP trong luồng Google login nếu backend yêu cầu.

### API
```http
POST /api/v1/auth/google/verify
```

### JSON
```json
{
  "email": "customer@example.com",
  "otp": "123456"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "accessToken": "JWT_ACCESS_TOKEN",
    "refreshToken": "REFRESH_TOKEN",
    "tokenType": "Bearer"
  },
  "message": "Logged in with Google successfully"
}
```

## 15. Admin - danh sách user

### Tên mô tả API
Admin xem toàn bộ user.

### API
```http
GET /api/v1/admin/users
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{}
```

### Post-response
```json
{
  "success": true,
  "data": []
}
```

## 16. Admin - tạo staff

### Tên mô tả API
Admin tạo tài khoản staff active, staff có thể đăng nhập bằng password đã cấp.

### API
```http
POST /api/v1/admin/users/staff
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "email": "staff@example.com",
  "password": "{{staffPassword}}",
  "fullName": "Staff One",
  "phone": "0900222333",
  "birthYear": 1999
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "email": "staff@example.com",
    "roles": ["STAFF"],
    "status": "ACTIVE"
  },
  "message": "Staff account created successfully"
}
```

## 17. Admin - đổi trạng thái user

### Tên mô tả API
Admin khóa/mở user.

### API
```http
PATCH /api/v1/admin/users/{{userId}}/status
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "status": "SUSPENDED"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "SUSPENDED"
  },
  "message": "User status updated successfully"
}
```

## Checklist hoàn tất Phase 2

- Register tạo pending registration, chưa tạo user thật trước khi verify email.
- Verify email tạo user thật và gán role `CUSTOMER`.
- Login trả Bearer access token, refresh token, expiresInMs và profile user.
- `/api/v1/users/me` yêu cầu JWT hợp lệ.
- User cập nhật profile và đổi mật khẩu được.
- Refresh token cấp access token mới.
- Logout thu hồi refresh token.
- Password reset request/confirm hoạt động.
- Admin route yêu cầu role `ADMIN`; customer không được truy cập `/api/v1/admin/**`.
- Admin tạo staff và cập nhật status user được.
