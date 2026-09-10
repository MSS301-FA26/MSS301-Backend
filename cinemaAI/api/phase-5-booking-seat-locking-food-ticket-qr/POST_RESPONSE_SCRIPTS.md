# Phase 5 - Post-response Scripts

Dan phan **Post-response** vao tab **Scripts -> Post-response** trong Postman.

## 1. Tao food item

### API
```http
POST /api/v1/admin/foods/items
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Popcorn M",
  "description": "Bap rang size M",
  "price": 30000,
  "imageUrl": "https://example.com/popcorn.jpg",
  "status": "ACTIVE"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tao food item thanh cong", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.exist;
});

pm.collectionVariables.set("foodItemId", body.data.id);
```

## 2. Tao food combo

### API
```http
POST /api/v1/admin/foods/combos
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Combo Popcorn + Drink",
  "description": "Combo bap nuoc",
  "price": 65000,
  "imageUrl": "https://example.com/combo.jpg",
  "status": "ACTIVE"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tao food combo thanh cong", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.exist;
});

pm.collectionVariables.set("foodComboId", body.data.id);
```

## 3. Tao rule gia ve

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
  "seatType": "STANDARD",
  "weekend": false,
  "holiday": false,
  "price": 95000,
  "active": true
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tao rule gia ve thanh cong", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("ticketPricingRuleId", body.data.id);
```

## 4. Validate gia ve

### API
```http
POST /api/v1/ticket-pricing/validate
```

### JSON
```json
{
  "showtimeId": {{showtimeId}},
  "comboId": null,
  "holiday": false,
  "tickets": [
    {
      "seatId": {{seatId}},
      "ticketType": "ADULT",
      "seatType": "STANDARD",
      "viewerAge": 22,
      "quantity": 1
    }
  ]
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Validate gia ve thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("ticketFinalAmount", body.data.finalAmount);
```

## 5. Hold ghe

### API
```http
POST /api/v1/bookings/hold
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "showtimeId": {{showtimeId}},
  "seatIds": [{{seatId}}],
  "holiday": false,
  "tickets": [
    {
      "seatId": {{seatId}},
      "ticketType": "ADULT",
      "seatType": "STANDARD",
      "viewerAge": 22,
      "quantity": 1
    }
  ],
  "foods": []
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Hold ghe thanh cong", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("HOLDING");
});

pm.collectionVariables.set("holdBookingId", body.data.id);
pm.collectionVariables.set("bookingCode", body.data.bookingCode);
pm.collectionVariables.set("bookingStatus", body.data.status);
```

## 6. Tao booking

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
  "comboId": null,
  "holiday": false,
  "tickets": [
    {
      "seatId": {{seatId}},
      "ticketType": "ADULT",
      "seatType": "STANDARD",
      "viewerAge": 22,
      "quantity": 1
    }
  ]
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tao booking thanh cong", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("PENDING_PAYMENT");
});

pm.collectionVariables.set("bookingId", body.data.id);
pm.collectionVariables.set("bookingCode", body.data.bookingCode);
pm.collectionVariables.set("bookingStatus", body.data.status);
pm.collectionVariables.set("bookingTotalAmount", body.data.totalAmount);
```

## 7. Mock payment de lay QR

### API
```http
POST /api/v1/payments/mock?bookingId={{bookingId}}
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Mock payment thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("SUCCESS");
});

pm.collectionVariables.set("paymentId", body.data.id);
pm.collectionVariables.set("paymentStatus", body.data.status);
```

Sau buoc nay goi `GET /api/v1/bookings/{{bookingId}}` hoac `GET /api/v1/admin/bookings/{{bookingId}}` de lay `qrCode`.

## 8. Lay QR tu chi tiet booking

### API
```http
GET /api/v1/bookings/{{bookingId}}
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Booking da thanh toan va co QR", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("PAID");
  pm.expect(body.data.qrCode).to.be.a("string").and.not.empty;
});

pm.collectionVariables.set("qrCode", body.data.qrCode);
pm.collectionVariables.set("bookingStatus", body.data.status);
```

## 9. Check-in QR

### API
```http
POST /api/v1/staff/check-in
Authorization: Bearer {{staffToken}}
```

Hoac:

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

pm.test("Check-in thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("USED");
});

pm.collectionVariables.set("bookingStatus", body.data.status);
pm.collectionVariables.set("checkinStatus", body.data.status);
```

## 10. Yeu cau hoan tien

### API
```http
POST /api/v1/bookings/{{bookingId}}/refund-request
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "reason": "Rap gap su co mat dien"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Yeu cau hoan tien thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("REFUND_REQUESTED");
});

pm.collectionVariables.set("bookingStatus", body.data.status);
pm.collectionVariables.set("refundReason", body.data.refundReason);
```

## 11. Admin xac nhan da hoan tien

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

pm.test("Xac nhan hoan tien thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("REFUNDED");
});

pm.collectionVariables.set("bookingStatus", body.data.status);
pm.collectionVariables.set("refundedAt", body.data.refundedAt);
```

## 12. Xoa mem food item

### API
```http
DELETE /api/v1/admin/foods/items/{{foodItemId}}
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Xoa mem food item thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("INACTIVE");
});
```
