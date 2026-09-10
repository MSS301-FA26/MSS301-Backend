# Post-response Scripts cho Postman

Các script này dùng trong tab **Scripts -> Post-response** của Postman. Mục tiêu là tự lưu token, id và dữ liệu quan trọng vào Environment sau khi gọi API.

## 1. Script dùng chung

Dán script này vào Post-response của các request cần debug nhanh response.

```javascript
let body = {};

try {
  body = pm.response.json();
} catch (error) {
  console.log("Response không phải JSON");
}

pm.test("Response status 2xx", function () {
  pm.expect(pm.response.code).to.be.within(200, 299);
});

pm.test("Response có field success", function () {
  pm.expect(body).to.have.property("success");
});

if (body.message) {
  pm.collectionVariables.set("lastMessage", body.message);
}
```

## 2. Đăng ký

API:

```http
POST /api/v1/auth/register
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Đăng ký thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

if (body.data?.user?.email) {
  pm.collectionVariables.set("customerEmail", body.data.user.email);
}

if (body.data?.emailVerificationRequired !== undefined) {
  pm.collectionVariables.set("emailVerificationRequired", body.data.emailVerificationRequired);
}
```

## 3. Xác minh email

API:

```http
POST /api/v1/auth/verify-email
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Xác minh email thành công", function () {
  pm.expect(pm.response.code).to.be.within(200, 299);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("emailVerified", true);
```

## 4. Đăng nhập

API:

```http
POST /api/v1/auth/login
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Đăng nhập thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.accessToken).to.not.be.empty;
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

Header các request cần auth:

```http
Authorization: Bearer {{accessToken}}
```

## 5. Refresh token

API:

```http
POST /api/v1/auth/refresh
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Refresh token thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("accessToken", body.data.accessToken);

if (body.data.refreshToken) {
  pm.collectionVariables.set("refreshToken", body.data.refreshToken);
}
```

## 6. Tạo rạp

API:

```http
POST /api/v1/admin/cinema
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Tạo rạp thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("cinemaId", body.data.id);
```

## 7. Tạo phòng

API:

```http
POST /api/v1/admin/rooms
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Tạo phòng thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("roomId", body.data.id);
pm.collectionVariables.set("rowCount", body.data.rowCount);
pm.collectionVariables.set("columnCount", body.data.columnCount);
```

## 8. Sinh ghế

API:

```http
POST /api/v1/admin/rooms/{{roomId}}/seats/generate
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Sinh ghế thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.length).to.be.greaterThan(0);
});

pm.collectionVariables.set("seatCount", body.data.length);

if (body.data[0]) {
  pm.collectionVariables.set("seatId", body.data[0].id);
  pm.collectionVariables.set("seatRowId", body.data[0].seatRowId);
  pm.collectionVariables.set("seatCode", `${body.data[0].rowLabel}${body.data[0].seatNumber}`);
}
```

## 9. Tạo phim

API:

```http
POST /api/v1/admin/movies
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Tạo phim thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("movieId", body.data.id);
pm.collectionVariables.set("movieTitle", body.data.title);
```

## 10. Tạo suất chiếu

API:

```http
POST /api/v1/admin/showtimes
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Tạo suất chiếu thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("showtimeId", body.data.id);
pm.collectionVariables.set("showtimeStartTime", body.data.startTime);
```

## 11. Hold ghế

API:

```http
POST /api/v1/bookings/hold
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Hold ghế thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("holdBookingId", body.data.id);
pm.collectionVariables.set("bookingCode", body.data.bookingCode);
```

## 12. Tạo booking

API:

```http
POST /api/v1/bookings
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Tạo booking thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("bookingId", body.data.id);
pm.collectionVariables.set("bookingCode", body.data.bookingCode);
pm.collectionVariables.set("qrCode", body.data.qrCode);
pm.collectionVariables.set("bookingStatus", body.data.status);
```

## 13. Check-in QR

API:

```http
POST /api/v1/admin/check-in
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Check-in thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("checkinStatus", body.data.status);
```

## 14. Tạo rule giá vé

API:

```http
POST /api/v1/admin/ticket-pricing/rules
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Tạo rule giá vé thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("ticketPricingRuleId", body.data.id);
```

## 15. Tạo combo vé

API:

```http
POST /api/v1/admin/ticket-pricing/combos
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Tạo combo vé thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("ticketComboId", body.data.id);
```

## 16. Tạo trailer interaction

API:

```http
POST /api/v1/recommendations/trailer-interactions
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Lưu tương tác trailer thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("trailerInteractionId", body.data.id);
```

## 17. Tạo actor

API:

```http
POST /api/v1/admin/actors
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Tạo actor thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("actorId", body.data.id);
pm.collectionVariables.set("actorName", body.data.name);
```

## 18. Tạo genre

API:

```http
POST /api/v1/admin/genres
```

Post-response script:

```javascript
const body = pm.response.json();

pm.test("Tạo genre thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("genreId", body.data.id);
pm.collectionVariables.set("genreName", body.data.name);
```
