# Phase 4 - Cinema, room, seat và showtime

Phase 4 theo mapping bao phủ quản lý rạp, phòng chiếu, sơ đồ ghế và lịch chiếu.
Đây là nền dữ liệu trực tiếp cho booking/seat locking ở phase sau.

## Quy tắc nghiệp vụ chính

- Scope hiện tại giới hạn một rạp chính trong hệ thống.
- Admin tạo/cập nhật/xóa mềm rạp, phòng, ghế và suất chiếu.
- Room name được chuẩn hóa khoảng trắng và không được trùng trong cùng rạp.
- Seat layout có thể sinh tự động theo `rowCount`/`columnCount` hoặc thay bằng layout custom theo từng hàng.
- Ghế đôi `COUPLE` phải có số ghế chẵn trong một hàng.
- Showtime chỉ được tạo cho phim và phòng hợp lệ, start time phải ở tương lai.
- Showtime mới chỉ được tạo với `SCHEDULED` hoặc `OPEN`.
- Phòng không được có suất chiếu chồng lịch, trừ suất đã `CANCELLED`.
- Không cập nhật/hủy/xóa showtime khi có booking active.
- Seat map public trả runtime status: `AVAILABLE`, `HOLDING`, `BOOKED`, `CHECKED_IN`, `UNAVAILABLE`.

## 1. Tạo rạp

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "CinemaAI Main",
    "address": "123 Nguyễn Trãi",
    "city": "Hồ Chí Minh",
    "phone": "0900999888",
    "status": "ACTIVE"
  },
  "message": "Cinema created successfully"
}
```

## 2. Lấy rạp hiện tại

### API public
```http
GET /api/v1/cinema
```

### API admin
```http
GET /api/v1/admin/cinema
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
    "name": "CinemaAI Main",
    "status": "ACTIVE"
  }
}
```

## 3. Cập nhật rạp

### API
```http
PUT /api/v1/admin/cinema
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "CinemaAI Main Updated",
  "address": "456 Lê Lợi",
  "city": "Hồ Chí Minh",
  "phone": "0900999889",
  "status": "ACTIVE"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "CinemaAI Main Updated"
  },
  "message": "Cinema updated successfully"
}
```

## 4. Tạo phòng chiếu

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "cinemaId": 1,
    "name": "Phòng 2D số 1",
    "roomType": "TWO_D",
    "rowCount": 3,
    "columnCount": 4,
    "status": "ACTIVE"
  },
  "message": "Room created successfully"
}
```

## 5. Danh sách phòng

### API public
```http
GET /api/v1/cinema/rooms
```

### API admin
```http
GET /api/v1/admin/rooms
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
  "data": [
    {
      "id": 1,
      "name": "Phòng 2D số 1",
      "roomType": "TWO_D",
      "status": "ACTIVE"
    }
  ]
}
```

## 6. Sinh sơ đồ ghế lần đầu

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
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "roomId": 1,
      "seatRowId": 1,
      "rowLabel": "A",
      "displayOrder": 1,
      "seatNumber": 1,
      "displayColumn": 1,
      "startColumn": 1,
      "seatType": "NORMAL",
      "status": "AVAILABLE"
    }
  ]
}
```

## 7. Thay toàn bộ sơ đồ ghế custom

### API
```http
PUT /api/v1/admin/rooms/{{roomId}}/seats
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "defaultSeatType": "STANDARD",
  "rows": [
    {
      "rowLabel": "A",
      "displayOrder": 1,
      "startColumn": 2,
      "seatType": "STANDARD",
      "seatNumbers": [18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
    },
    {
      "rowLabel": "O",
      "displayOrder": 14,
      "startColumn": 5,
      "seatType": "COUPLE",
      "seatNumbers": [6, 5, 4, 3, 2, 1]
    }
  ]
}
```

### Post-response
```json
{
  "success": true,
  "data": [
    {
      "rowLabel": "A",
      "seatNumber": 18,
      "displayColumn": 2,
      "seatType": "STANDARD"
    }
  ]
}
```

## 8. Cập nhật một ghế

### API
```http
PUT /api/v1/admin/rooms/seats/{{seatId}}
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "seatType": "VIP",
  "status": "MAINTENANCE"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "seatType": "VIP",
    "status": "MAINTENANCE"
  }
}
```

## 9. Tạo suất chiếu

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
```json
{
  "success": true,
  "data": {
    "id": 1,
    "movieId": 1,
    "roomId": 1,
    "startTime": "2026-07-01T19:00:00",
    "endTime": "2026-07-01T21:05:00",
    "basePrice": 90000,
    "status": "OPEN"
  },
  "message": "Showtime created successfully"
}
```

## 10. Tạo bulk suất chiếu

### API
```http
POST /api/v1/admin/showtimes/bulk
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "movieId": {{movieId}},
  "basePrice": 90000,
  "defaultStatus": "OPEN",
  "slots": [
    {
      "roomId": {{roomId}},
      "startTime": "2026-07-02T09:00:00"
    },
    {
      "roomId": {{roomId}},
      "startTime": "2026-07-02T14:30:00",
      "status": "SCHEDULED"
    }
  ]
}
```

### Post-response
```json
{
  "success": true,
  "data": [
    {
      "id": 2,
      "status": "OPEN"
    },
    {
      "id": 3,
      "status": "SCHEDULED"
    }
  ],
  "message": "2 showtime(s) created successfully"
}
```

## 11. Tìm suất chiếu public

### API
```http
GET /api/v1/showtimes?movieId={{movieId}}&date=2026-07-01
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
      "movieId": 1,
      "roomId": 1,
      "status": "OPEN"
    }
  ]
}
```

## 12. Xem seat map theo suất chiếu

### API
```http
GET /api/v1/showtimes/{{showtimeId}}/seat-map
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
    "showtime": {
      "id": 1,
      "movieTitle": "Action Movie A",
      "roomName": "Phòng 2D số 1"
    },
    "rowCount": 3,
    "columnCount": 4,
    "seats": [
      {
        "id": 1,
        "rowLabel": "A",
        "seatNumber": 1,
        "runtimeStatus": "AVAILABLE"
      }
    ]
  }
}
```

## 13. Cập nhật suất chiếu

### API
```http
PUT /api/v1/admin/showtimes/{{showtimeId}}
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "movieId": {{movieId}},
  "roomId": {{roomId}},
  "startTime": "2026-07-01T22:00:00",
  "basePrice": 99000,
  "status": "OPEN"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "basePrice": 99000
  },
  "message": "Showtime updated successfully"
}
```

## 14. Đổi trạng thái suất chiếu

### API
```http
PATCH /api/v1/admin/showtimes/{{showtimeId}}/status?status=CANCELLED
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
    "status": "CANCELLED"
  },
  "message": "Showtime status updated successfully"
}
```

## Checklist hoàn tất Phase 4

- Admin quản lý được rạp chính.
- Public xem được rạp và danh sách phòng active.
- Admin tạo/cập nhật room và đổi status room được.
- Admin sinh seat layout mặc định và thay toàn bộ layout custom được.
- Rule số hàng/cột và ghế đôi được validate.
- Admin tạo showtime đơn và bulk showtime được.
- Showtime conflict trong cùng phòng được chặn.
- Public tìm showtime theo phim/ngày và xem seat map được.
- Seat map phản ánh trạng thái ghế vật lý và runtime booking status.
- Showtime có state transition hợp lệ và không cho cancel/update khi có booking active.
