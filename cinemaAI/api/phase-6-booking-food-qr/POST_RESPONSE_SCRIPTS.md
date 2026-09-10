# Phase 6 - Post-response Scripts

Dán phần **Post-response** vào tab **Scripts -> Post-response** trong Postman.

## 1. Tạo food item

### Tên mô tả API
Admin tạo món ăn bán kèm booking và lưu `foodItemId`.

### API
```http
POST /api/v1/admin/foods/items
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Popcorn",
  "description": "Bắp rang size M",
  "price": 30000,
  "imageUrl": "https://example.com/popcorn.jpg",
  "status": "ACTIVE"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo food item thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("foodItemId", body.data.id);
```

## 2. Tạo food combo

### Tên mô tả API
Admin tạo combo đồ ăn và lưu `foodComboId`.

### API
```http
POST /api/v1/admin/foods/combos
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Combo Popcorn + Drink",
  "description": "Combo bắp nước",
  "price": 65000,
  "status": "ACTIVE"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo food combo thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("foodComboId", body.data.id);
```

## 3. Hold ghế

### Tên mô tả API
User khóa ghế tạm thời trước khi thanh toán.

### API
```http
POST /api/v1/bookings/hold
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "showtimeId": {{showtimeId}},
  "seatIds": [{{seatId}}]
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Hold ghế thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("holdBookingId", body.data.id);
pm.collectionVariables.set("bookingCode", body.data.bookingCode);
pm.collectionVariables.set("bookingStatus", body.data.status);
```

## 4. Tạo booking

### Tên mô tả API
User tạo booking từ hold, chọn vé và F&B nếu có.

### API
```http
POST /api/v1/bookings
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "holdBookingId": {{holdBookingId}},
  "foods": [
    {
      "foodItemId": {{foodItemId}},
      "foodComboId": null,
      "quantity": 1
    }
  ],
  "tickets": [
    {
      "ticketType": "ADULT",
      "viewerAge": 22,
      "quantity": 1
    }
  ]
}
```

### Post-response
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
pm.collectionVariables.set("bookingTotalAmount", body.data.totalAmount);
```

## 5. Check-in QR

### Tên mô tả API
Staff/admin quét QR để check-in vé. `qrCode` được sinh ở bước 4 khi user tạo booking thành công.

### API
```http
POST /api/v1/admin/check-in
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "qrCode": "{{qrCode}}"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Check-in thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("checkinStatus", body.data.status);
```

## 6. Yêu cầu hoàn tiền

### Tên mô tả API
User hoặc admin tạo yêu cầu hoàn tiền cho booking. Chỉ dùng booking đang `PAID` hoặc `CANCELLED`; booking đã check-in sẽ là `USED` và không được yêu cầu hoàn tiền.

### API
```http
POST /api/v1/bookings/{{bookingId}}/refund-request
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "reason": "Cúp điện trong rạp"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Yêu cầu hoàn tiền thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("bookingStatus", body.data.status);
pm.collectionVariables.set("refundReason", body.data.refundReason);
```

## 7. Admin xác nhận đã hoàn tiền

### Tên mô tả API
Admin đánh dấu booking đã hoàn tiền.

### API
```http
POST /api/v1/admin/bookings/{{bookingId}}/mark-refunded
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Xác nhận hoàn tiền thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("bookingStatus", body.data.status);
pm.collectionVariables.set("refundedAt", body.data.refundedAt);
```
