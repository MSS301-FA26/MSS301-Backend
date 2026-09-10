# Phase 6 - Post-response Scripts

Dan phan **Post-response** vao tab **Scripts -> Post-response** trong Postman.

## 1. Tao VNPay payment URL

### API
```http
POST /api/v1/payments/vnpay/create?bookingId={{bookingId}}
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tao VNPay payment URL thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.provider).to.eql("VNPAY");
  pm.expect(body.data.status).to.eql("PENDING");
  pm.expect(body.data.paymentUrl).to.be.a("string").and.not.empty;
});

pm.collectionVariables.set("paymentId", body.data.id);
pm.collectionVariables.set("paymentProvider", body.data.provider);
pm.collectionVariables.set("paymentStatus", body.data.status);
pm.collectionVariables.set("paymentUrl", body.data.paymentUrl);

const txnRef = new URL(body.data.paymentUrl).searchParams.get("vnp_TxnRef");
if (txnRef) {
  pm.collectionVariables.set("vnpTxnRef", txnRef);
}
```

## 2. Mock payment

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
  pm.expect(body.data.provider).to.eql("MOCK");
  pm.expect(body.data.status).to.eql("SUCCESS");
  pm.expect(body.data.transactionId).to.be.a("string").and.not.empty;
});

pm.collectionVariables.set("paymentId", body.data.id);
pm.collectionVariables.set("paymentProvider", body.data.provider);
pm.collectionVariables.set("paymentStatus", body.data.status);
pm.collectionVariables.set("transactionId", body.data.transactionId);
```

## 3. Tra payment theo booking

### API
```http
GET /api/v1/payments/booking/{{bookingId}}
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Lay payment theo booking thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.bookingId).to.eql(Number(pm.collectionVariables.get("bookingId")));
});

pm.collectionVariables.set("paymentId", body.data.id);
pm.collectionVariables.set("paymentStatus", body.data.status);
pm.collectionVariables.set("paymentProvider", body.data.provider);
```

## 4. Lay QR sau payment

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

pm.test("Booking da PAID va co QR", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("PAID");
  pm.expect(body.data.qrCode).to.be.a("string").and.not.empty;
});

pm.collectionVariables.set("bookingStatus", body.data.status);
pm.collectionVariables.set("qrCode", body.data.qrCode);
```

## 5. Customer yeu cau refund

### API
```http
POST /api/v1/bookings/{{bookingId}}/refund-request
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "reason": "Khach yeu cau hoan tien"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Yeu cau refund thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("REFUND_REQUESTED");
});

pm.collectionVariables.set("bookingStatus", body.data.status);
pm.collectionVariables.set("refundReason", body.data.refundReason);
```

## 6. Admin danh dau da refund

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

pm.test("Admin mark refunded thanh cong", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.eql("REFUNDED");
});

pm.collectionVariables.set("bookingStatus", body.data.status);
pm.collectionVariables.set("refundedAt", body.data.refundedAt);
```
