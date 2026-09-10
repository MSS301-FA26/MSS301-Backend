# Phase 4 - Post-response Scripts

Dán phần **Post-response** vào tab **Scripts -> Post-response** trong Postman.

## 1. Tạo rạp

### API
```http
POST /api/v1/admin/cinema
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo rạp thành công hoặc bị giới hạn một rạp", function () {
  pm.expect(pm.response.code).to.be.oneOf([201, 409]);
});

if (pm.response.code === 201) {
  pm.expect(body.success).to.eql(true);
  pm.collectionVariables.set("cinemaId", body.data.id);
  pm.collectionVariables.set("cinemaName", body.data.name);
}
```

## 2. Lấy rạp hiện tại

### API
```http
GET /api/v1/admin/cinema
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Lấy rạp hiện tại thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.not.eql(undefined);
});

pm.collectionVariables.set("cinemaId", body.data.id);
pm.collectionVariables.set("cinemaName", body.data.name);
```

## 3. Cập nhật rạp

### API
```http
PUT /api/v1/admin/cinema
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Cập nhật rạp thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.eql(Number(pm.collectionVariables.get("cinemaId")));
});

pm.collectionVariables.set("cinemaName", body.data.name);
```

## 4. Tạo phòng chiếu

### API
```http
POST /api/v1/admin/rooms
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo phòng thành công", function () {
  pm.expect(pm.response.code).to.eql(201);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.not.eql(undefined);
});

pm.collectionVariables.set("roomId", body.data.id);
pm.collectionVariables.set("rowCount", body.data.rowCount);
pm.collectionVariables.set("columnCount", body.data.columnCount);
```

## 5. Danh sách phòng

### API
```http
GET /api/v1/admin/rooms
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Danh sách phòng thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data).to.be.an("array");
});
```

## 6. Sinh sơ đồ ghế

### API
```http
POST /api/v1/admin/rooms/{{roomId}}/seats/generate
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Sinh sơ đồ ghế thành công", function () {
  pm.expect(pm.response.code).to.eql(201);
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

## 7. Thay toàn bộ sơ đồ ghế custom

### API
```http
PUT /api/v1/admin/rooms/{{roomId}}/seats
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Thay sơ đồ ghế thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.length).to.be.greaterThan(0);
});

pm.collectionVariables.set("seatCount", body.data.length);

if (body.data[0]) {
  pm.collectionVariables.set("seatId", body.data[0].id);
}
```

## 8. Cập nhật một ghế

### API
```http
PUT /api/v1/admin/rooms/seats/{{seatId}}
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Cập nhật ghế thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.eql(Number(pm.collectionVariables.get("seatId")));
});
```

## 9. Tạo suất chiếu

### API
```http
POST /api/v1/admin/showtimes
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo suất chiếu thành công", function () {
  pm.expect(pm.response.code).to.eql(201);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.not.eql(undefined);
});

pm.collectionVariables.set("showtimeId", body.data.id);
pm.collectionVariables.set("showtimeStartTime", body.data.startTime);
```

## 10. Tạo bulk suất chiếu

### API
```http
POST /api/v1/admin/showtimes/bulk
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo bulk suất chiếu thành công", function () {
  pm.expect(pm.response.code).to.eql(201);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data).to.be.an("array");
});

if (body.data?.[0]?.id) {
  pm.collectionVariables.set("bulkShowtimeId", body.data[0].id);
}
```

## 11. Tìm suất chiếu public

### API
```http
GET /api/v1/showtimes?movieId={{movieId}}&date=2026-07-01
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tìm suất chiếu public thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data).to.be.an("array");
});

if (body.data?.[0]?.id) {
  pm.collectionVariables.set("foundShowtimeId", body.data[0].id);
}
```

## 12. Xem seat map

### API
```http
GET /api/v1/showtimes/{{showtimeId}}/seat-map
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Lấy seat map thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.seats).to.be.an("array");
});

const firstSeat = body.data.seats?.[0];

if (firstSeat) {
  pm.collectionVariables.set("seatId", firstSeat.seatId || firstSeat.id);
  pm.collectionVariables.set("seatCode", `${firstSeat.rowLabel}${firstSeat.seatNumber}`);
  pm.collectionVariables.set("seatRuntimeStatus", firstSeat.runtimeStatus);
}
```

## 13. Cập nhật suất chiếu

### API
```http
PUT /api/v1/admin/showtimes/{{showtimeId}}
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Cập nhật suất chiếu thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.eql(Number(pm.collectionVariables.get("showtimeId")));
});

pm.collectionVariables.set("showtimeStartTime", body.data.startTime);
```

## 14. Đổi trạng thái suất chiếu

### API
```http
PATCH /api/v1/admin/showtimes/{{showtimeId}}/status?status=CANCELLED
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Đổi trạng thái suất chiếu thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.not.be.empty;
});

pm.collectionVariables.set("showtimeStatus", body.data.status);
```
