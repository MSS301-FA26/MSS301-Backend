# Phase 6 - Payment

Phase nay dung sau Phase 5. Can co `bookingId` o trang thai `HOLDING` hoac `PENDING_PAYMENT`.

## 1. Tao VNPay payment URL

### Ten mo ta API
Customer tao giao dich VNPay cho booking cua minh. Backend tao record `Payment` provider `VNPAY`, status `PENDING`, sinh `paymentUrl` va chuyen booking `HOLDING` sang `PENDING_PAYMENT` neu can.

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "bookingId": 10,
    "provider": "VNPAY",
    "transactionId": null,
    "amount": 125000,
    "status": "PENDING",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
    "paidAt": null,
    "createdAt": "2026-06-13T22:30:00"
  },
  "message": "Payment URL created"
}
```

## 2. VNPay return callback

### Ten mo ta API
VNPay redirect browser ve backend sau khi user thanh toan. Backend verify signature, cap nhat payment `SUCCESS`/`FAILED`, sau do redirect FE ve `PAYMENT_SUCCESS_REDIRECT_URL` kem query goc.

### API
```http
GET /api/v1/payments/vnpay/return?vnp_TxnRef={{vnpTxnRef}}&vnp_ResponseCode=00&vnp_TransactionNo={{transactionNo}}&vnp_SecureHash={{secureHash}}
```

### JSON
```json
{}
```

### Post-response
```http
302 Location: {{PAYMENT_SUCCESS_REDIRECT_URL}}?...
```

## 3. VNPay IPN callback

### Ten mo ta API
VNPay server goi IPN de backend confirm payment. Backend verify signature, check amount, idempotency va tra JSON string theo format VNPay.

### API
```http
GET /api/v1/payments/vnpay/ipn?vnp_TxnRef={{vnpTxnRef}}&vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_Amount={{amountTimes100}}&vnp_TransactionNo={{transactionNo}}&vnp_SecureHash={{secureHash}}
```

### JSON
```json
{}
```

### Post-response
```json
{
  "RspCode": "00",
  "Message": "Confirm success"
}
```

## 4. Mock payment

### Ten mo ta API
Dev/test endpoint de confirm booking khong qua cong thanh toan that. Khi thanh cong, payment `SUCCESS`, booking `PAID`, ghe `BOOKED`, QR code duoc sinh va loyalty point duoc cong.

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
```json
{
  "success": true,
  "data": {
    "id": 2,
    "bookingId": 10,
    "provider": "MOCK",
    "transactionId": "MOCK-1780000000000",
    "amount": 125000,
    "status": "SUCCESS",
    "paymentUrl": null,
    "paidAt": "2026-06-13T22:35:00"
  },
  "message": "Payment confirmed"
}
```

## 5. Tra cuu payment theo booking

### Ten mo ta API
Customer tra cuu payment cua booking thuoc ve minh.

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
```json
{
  "success": true,
  "data": {
    "id": 2,
    "bookingId": 10,
    "provider": "MOCK",
    "transactionId": "MOCK-1780000000000",
    "amount": 125000,
    "status": "SUCCESS",
    "paidAt": "2026-06-13T22:35:00"
  }
}
```

## 6. Refund thu cong qua booking admin

### Ten mo ta API
Phase hien tai ho tro refund nghiep vu o muc booking: customer/admin tao refund request, admin danh dau da hoan tien. VNPay Refund API tu dong chua nam trong scope hien tai.

### API
```http
POST /api/v1/bookings/{{bookingId}}/refund-request
Authorization: Bearer {{accessToken}}

POST /api/v1/admin/bookings/{{bookingId}}/refund-request
Authorization: Bearer {{adminToken}}

POST /api/v1/admin/bookings/{{bookingId}}/mark-refunded
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "reason": "Khach yeu cau hoan tien"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 10,
    "status": "REFUNDED",
    "refundReason": "Khach yeu cau hoan tien",
    "refundedAt": "2026-06-13T22:40:00"
  },
  "message": "Booking marked as refunded successfully"
}
```

## 7. Rule nghiep vu can test

- Chi booking `HOLDING` hoac `PENDING_PAYMENT` moi duoc thanh toan.
- Khong tao them VNPay payment neu booking da co payment `PENDING`.
- Payment success phai chuyen booking sang `PAID`, sinh QR va giu ghe `BOOKED`.
- VNPay return/IPN phai verify signature.
- VNPay IPN phai idempotent: payment da `SUCCESS` thi tra `RspCode=02`.
- VNPay IPN phai check amount: sai amount tra `RspCode=04`.
- Refund provider tu dong la future enhancement; hien tai dung refund request + mark refunded.
