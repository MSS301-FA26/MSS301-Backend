# Phase 5 - Booking, seat locking, F&B va ticket QR

Phase nay dung sau Phase 4: can co `showtimeId` va `seatId` tu so do ghe cua suat chieu.

## 1. Public lay food items dang ban

### Ten mo ta API
Lay danh sach mon le dang `ACTIVE` de user chon khi tao booking.

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
      "description": "Bap rang size M",
      "price": 30000,
      "imageUrl": "https://example.com/popcorn.jpg",
      "status": "ACTIVE"
    }
  ]
}
```

## 2. Public lay food combos dang ban

### Ten mo ta API
Lay danh sach combo F&B dang `ACTIVE`.

### API
```http
GET /api/v1/foods/combos
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
      "name": "Combo Popcorn + Drink",
      "price": 65000,
      "status": "ACTIVE"
    }
  ]
}
```

## 3. Validate gia ve truoc booking

### Ten mo ta API
Tinh truoc gia ve theo suat chieu, loai ve, tuoi nguoi xem, loai ghe va co ngay le hay khong.

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
```json
{
  "success": true,
  "data": {
    "subtotal": 95000,
    "discountAmount": 0,
    "finalAmount": 95000,
    "lines": [
      {
        "ticketType": "ADULT",
        "viewerAge": 22,
        "quantity": 1,
        "unitPrice": 95000,
        "lineTotal": 95000
      }
    ]
  },
  "message": "Ticket price validated successfully"
}
```

## 4. Hold ghe

### Ten mo ta API
Customer khoa ghe tam thoi trong mot suat chieu. Ghe dang `HOLDING`, `BOOKED` hoac da check-in khong duoc hold trung.

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "bookingCode": "BK9CA9C2B8B701",
    "status": "HOLDING",
    "holdExpiresAt": "2026-06-13T20:10:00",
    "seats": [
      {
        "seatId": 1,
        "rowLabel": "A",
        "seatNumber": 1,
        "status": "HOLDING"
      }
    ]
  },
  "message": "Seats held successfully"
}
```

## 5. Tao booking tu hold

### Ten mo ta API
Customer xac nhan booking tu `holdBookingId`, backend tinh tien ve/F&B, chuyen booking sang `PENDING_PAYMENT` va giu ghe o trang thai `BOOKED`. QR code chi duoc sinh sau khi payment thanh cong.

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "bookingCode": "BK9CA9C2B8B701",
    "status": "PENDING_PAYMENT",
    "subtotal": 125000,
    "totalAmount": 125000,
    "qrCode": null,
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

## 6. Xem booking cua toi

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
      "bookingCode": "BK9CA9C2B8B701",
      "status": "PAID",
      "totalAmount": 125000
    }
  ]
}
```

## 7. Xem chi tiet booking cua toi

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "bookingCode": "BK9CA9C2B8B701",
    "status": "PAID",
    "qrCode": "CINEAI:BK9CA9C2B8B701:2"
  }
}
```

## 8. Staff/Admin check-in bang QR

### Ten mo ta API
Staff hoac admin quet QR de xac nhan khach vao rap. Booking phai da thanh toan (`PAID`) va co `qrCode`; booking hop le se chuyen sang `USED`, ghe chuyen sang `CHECKED_IN`.

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "USED",
    "seats": [
      {
        "seatId": 1,
        "status": "CHECKED_IN"
      }
    ]
  },
  "message": "Ticket checked in successfully"
}
```

## 9. Admin check-in bang booking id

### API
```http
POST /api/v1/admin/bookings/{{bookingId}}/check-in
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "qrCode": "{{qrCode}}"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "USED"
  },
  "message": "Booking checked in successfully"
}
```

## 10. Huy booking cua toi

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

## 11. Yeu cau hoan tien

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "REFUND_REQUESTED",
    "refundRequestedAt": "2026-06-13T20:30:00",
    "refundReason": "Rap gap su co mat dien"
  },
  "message": "Refund requested successfully"
}
```

## 12. Admin quan ly booking va hoan tien thu cong

### API
```http
GET /api/v1/admin/bookings?status=PAID
GET /api/v1/admin/bookings/{{bookingId}}
DELETE /api/v1/admin/bookings/{{bookingId}}
POST /api/v1/admin/bookings/{{bookingId}}/refund-request
POST /api/v1/admin/bookings/{{bookingId}}/mark-refunded
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "reason": "Admin tao refund request thay customer"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "REFUNDED",
    "refundedAt": "2026-06-13T20:35:00"
  },
  "message": "Booking marked as refunded successfully"
}
```

## 13. Admin quan ly F&B

### API
```http
GET /api/v1/admin/foods/items
GET /api/v1/admin/foods/combos
POST /api/v1/admin/foods/items
POST /api/v1/admin/foods/combos
PUT /api/v1/admin/foods/items/{{foodItemId}}
PUT /api/v1/admin/foods/combos/{{foodComboId}}
DELETE /api/v1/admin/foods/items/{{foodItemId}}
DELETE /api/v1/admin/foods/combos/{{foodComboId}}
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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Popcorn M",
    "price": 30000,
    "status": "ACTIVE"
  }
}
```

## 14. Admin quan ly quy tac gia ve

### API
```http
GET /api/v1/admin/ticket-pricing/rules?ticketType=ADULT&roomType=TWO_D&active=true&page=0&size=20
POST /api/v1/admin/ticket-pricing/rules
PUT /api/v1/admin/ticket-pricing/rules/{{ruleId}}
DELETE /api/v1/admin/ticket-pricing/rules/{{ruleId}}
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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "ticketType": "ADULT",
    "roomType": "TWO_D",
    "seatType": "STANDARD",
    "price": 95000,
    "active": true
  },
  "message": "Ticket pricing rule created successfully"
}
```

## 15. Rule nghiep vu can test

- Hold ghe tra `409 Conflict` neu ghe da bi user khac hold/book.
- Hold hoac booking `PENDING_PAYMENT` het han se duoc scheduler/service release ve `EXPIRED` va ghe `RELEASED`.
- Tao booking can so ve khop so ghe da hold khi co `tickets`.
- Validate tuoi phai hop le voi `TicketType` va age rating cua phim.
- QR duoc sinh sau khi payment thanh cong va chi check-in duoc mot lan.
- Booking `USED` khong duoc refund request.
- Ticket combo pricing hien khong dung cho booking; F&B combo nam o `/api/v1/foods/combos`.
