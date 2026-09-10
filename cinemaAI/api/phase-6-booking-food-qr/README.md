# Phase 6 - Booking, khóa ghế, F&B và QR vé

## 1. Lấy danh sách đồ ăn

### Tên mô tả API
Lấy danh sách đồ ăn đang active.

### API
```http
GET /api/v1/foods/items
```

### JSON
```json
{}
```

### Post-response
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Popcorn",
      "price": 45000,
      "status": "ACTIVE"
    }
  ]
}
```

## 2. Giữ ghế

### Tên mô tả API
Customer giữ ghế tạm thời trong một suất chiếu.

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "bookingCode": "CINEAI-ABC123",
    "status": "HOLDING",
    "holdExpiresAt": "2026-07-01T18:10:00",
    "seats": [
      {"seatId": 1, "status": "HOLDING"},
      {"seatId": 2, "status": "HOLDING"}
    ]
  },
  "message": "Seats held successfully"
}
```

## 3. Tạo booking

### Tên mô tả API
Customer tạo booking từ hold, thêm F&B nếu có, backend tính tổng tiền và sinh QR.

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
      "quantity": 2
    }
  ]
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "bookingCode": "CINEAI-ABC123",
    "status": "PAID",
    "subtotal": 270000,
    "totalAmount": 270000,
    "qrCode": "CINEAI:CINEAI-ABC123:2"
  },
  "message": "Booking created successfully"
}
```

## 4. Tạo booking có loại vé và validate tuổi

### Tên mô tả API
Customer tạo booking từ hold kèm danh sách vé. Backend kiểm tra số vé khớp số ghế đã giữ, validate tuổi theo `TicketType` và age rating của phim, sau đó lưu `BookingTicket`.

### API
```http
POST /api/v1/bookings
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "holdBookingId": {{holdBookingId}},
  "comboId": null,
  "holiday": false,
  "tickets": [
    {
      "ticketType": "ADULT",
      "viewerAge": 22,
      "quantity": 1
    }
  ],
  "foods": []
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "bookingCode": "BK9CA9C2B8B701",
    "status": "PAID",
    "subtotal": 95000,
    "totalAmount": 95000,
    "qrCode": "CINEAI:BK9CA9C2B8B701:2",
    "tickets": [
      {
        "ticketType": "ADULT",
        "viewerAge": 22,
        "quantity": 1,
        "unitPrice": 95000,
        "lineTotal": 95000
      }
    ]
  },
  "message": "Booking created successfully"
}
```

## 5. Xem booking của tôi

### Tên mô tả API
Customer xem danh sách booking của mình.

### API
```http
GET /api/v1/bookings
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
  "data": [
    {
      "id": 1,
      "bookingCode": "CINEAI-ABC123",
      "status": "PAID",
      "totalAmount": 270000
    }
  ]
}
```

## 6. Hủy booking

### Tên mô tả API
Customer hủy booking của mình.

### API
```http
DELETE /api/v1/bookings/{{bookingId}}
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
    "status": "CANCELLED"
  },
  "message": "Booking cancelled successfully"
}
```

## 7. Yêu cầu hoàn tiền

### Tên mô tả API
Customer yêu cầu hoàn tiền cho booking đã thanh toán hoặc đã bị hủy, ví dụ do cúp điện hoặc sự cố vận hành. Booking đã check-in có trạng thái `USED` và không được yêu cầu hoàn tiền.

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "REFUND_REQUESTED",
    "refundRequestedAt": "2026-06-02T22:30:00",
    "refundReason": "Cúp điện trong rạp"
  },
  "message": "Refund requested successfully"
}
```

## 8. Admin đánh dấu đã hoàn tiền

### Tên mô tả API
Admin xác nhận booking đã được hoàn tiền sau khi xử lý thủ công hoặc qua payment provider ở phase sau.

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "REFUNDED",
    "refundedAt": "2026-06-02T22:35:00",
    "refundReason": "Cúp điện trong rạp"
  },
  "message": "Booking marked as refunded successfully"
}
```
