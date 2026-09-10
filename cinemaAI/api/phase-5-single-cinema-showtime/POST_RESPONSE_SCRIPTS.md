# Phase 5 - Post-response Scripts

Dán phần **Post-response** vào tab **Scripts -> Post-response** trong Postman.

## 1. Tạo rạp

### Tên mô tả API
Admin tạo rạp chính của hệ thống và lưu `cinemaId`.

### API
```http
POST /api/v1/admin/cinema
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "CinemaAI Main",
  "address": "123 Nguyễn Trãi",
  "city": "Hồ Chí Minh",
  "phone": "0900999888",
  "status": "ACTIVE"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo rạp thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("cinemaId", body.data.id);
```

## 1.1. Lấy rạp hiện tại

### Tên mô tả API
Lấy rạp đã có trong hệ thống và lưu `cinemaId`. Dùng bước này khi API tạo rạp trả `409 System is limited to one cinema`.

### API
```http
GET /api/v1/admin/cinema
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Lấy rạp hiện tại thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.exist;
});

const cinema = body.data;

pm.collectionVariables.set("cinemaId", cinema.id);
pm.collectionVariables.set("cinemaName", cinema.name);
```

## 2. Tạo phòng

### Tên mô tả API
Admin tạo phòng chiếu thuộc rạp hiện tại và lưu `roomId`. Không cần nhập `cinemaId`; backend tự lấy rạp đã cấu hình và database tự sinh ID phòng.

### API
```http
POST /api/v1/admin/rooms
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Phòng 2D số 1",
  "roomType": "TWO_D",
  "rowCount": 3,
  "columnCount": 4,
  "status": "ACTIVE"
}
```

### Post-response
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

## 3. Sinh ghế

### Tên mô tả API
Tạo sơ đồ ghế lần đầu, sau đó lưu `seatId` đầu tiên để test booking. Nếu cần thay toàn bộ sơ đồ đã có, dùng `PUT /api/v1/admin/rooms/{{roomId}}/seats`.

### API
```http
POST /api/v1/admin/rooms/{{roomId}}/seats/generate
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "defaultSeatType": "NORMAL"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Sinh ghế thành công", function () {
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

## 4. Tạo suất chiếu

### Tên mô tả API
Admin tạo suất chiếu và lưu `showtimeId`.

### API
```http
POST /api/v1/admin/showtimes
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "movieId": {{movieId}},
  "roomId": {{roomId}},
  "startTime": "2026-07-01T19:00:00",
  "basePrice": 90000,
  "status": "OPEN"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo suất chiếu thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("showtimeId", body.data.id);
pm.collectionVariables.set("showtimeStartTime", body.data.startTime);
```

## 5. Xem sơ đồ ghế

### Tên mô tả API
Lấy seat map theo suất chiếu và lưu ghế đầu tiên đang available.

### API
```http
GET /api/v1/showtimes/{{showtimeId}}/seat-map
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Lấy seat map thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

const firstSeat = body.data.seats?.[0];

if (firstSeat) {
  pm.collectionVariables.set("seatId", firstSeat.seatId || firstSeat.id);
  pm.collectionVariables.set("seatCode", `${firstSeat.rowLabel}${firstSeat.seatNumber}`);
}
```

## 6. Tạo rule giá vé

### Tên mô tả API
Admin tạo rule giá vé theo loại vé, loại phòng, cuối tuần và ngày lễ.

### API
```http
POST /api/v1/admin/ticket-pricing/rules
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "ticketType": "ADULT",
  "roomType": "TWO_D",
  "weekend": false,
  "holiday": false,
  "price": 95000,
  "active": true
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo rule giá vé thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("ticketPricingRuleId", body.data.id);
```

## 7. Tạo combo vé

### Tên mô tả API
Admin tạo combo vé và lưu `ticketComboId`.

### API
```http
POST /api/v1/admin/ticket-pricing/combos
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Combo 2 người lớn 1 trẻ em",
  "description": "Combo gia đình gồm 2 vé người lớn và 1 vé trẻ em.",
  "adultCount": 2,
  "childCount": 1,
  "seniorCount": 0,
  "studentCount": 0,
  "price": 250000,
  "active": true
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo combo vé thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("ticketComboId", body.data.id);
```

## 8. Validate giá vé

### Tên mô tả API
Validate tuổi, loại vé và tổng tiền trước khi booking.

### API
```http
POST /api/v1/ticket-pricing/validate
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "showtimeId": {{showtimeId}},
  "comboId": {{ticketComboId}},
  "holiday": false,
  "tickets": [
    {
      "ticketType": "ADULT",
      "viewerAge": 30,
      "quantity": 1
    }
  ]
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Validate giá vé thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("ticketFinalAmount", body.data.finalAmount);
pm.collectionVariables.set("ticketEligible", body.data.eligible);
```
