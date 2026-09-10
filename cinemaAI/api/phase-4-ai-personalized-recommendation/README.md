# Phase 4 - AI gợi ý phim cá nhân hóa

## 1. Ghi nhận hành vi xem trailer

### Tên mô tả API
Ghi nhận user click, xem, bỏ qua hoặc xem hết trailer. Nếu user xem hết trailer phim hành động, hệ thống tăng điểm thể loại, diễn viên và đạo diễn của phim đó.

### API
```http
POST /api/v1/recommendations/trailer-interactions
Authorization: Bearer {{accessToken}}
```

### JSON
```json
{
  "movieId": {{movieId}},
  "interactionType": "COMPLETE",
  "watchedSeconds": 120,
  "totalSeconds": 120
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "movieId": {{movieId}},
    "movieTitle": "Action Movie A",
    "interactionType": "COMPLETE",
    "watchedSeconds": 120,
    "totalSeconds": 120
  },
  "message": "Trailer interaction recorded successfully"
}
```

## 2. Làm mới hồ sơ sở thích

### Tên mô tả API
Tổng hợp sở thích từ trailer, lịch sử booking và review.

### API
```http
POST /api/v1/recommendations/preferences/refresh
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
    "userId": 2,
    "cohortKey": "general",
    "genreScores": {"1": 5.0},
    "actorScores": {"1": 5.0},
    "directorScores": {"action director": 5.0}
  },
  "message": "Preference profile refreshed"
}
```

## 3. Lấy danh sách phim gợi ý

### Tên mô tả API
Lấy phim gợi ý theo hồ sơ sở thích của user.

### API
```http
GET /api/v1/recommendations/movies?limit=10
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
      "movieId": 2,
      "title": "Action Movie New Release",
      "ageRating": "13+",
      "director": "Action Director",
      "releaseDate": "2026-07-10",
      "score": 11.25,
      "reasons": [
        "Matches preferred genre: Action",
        "Includes favorite actor: Favorite Star",
        "Matches preferred director: Action Director"
      ]
    }
  ]
}
```

## 4. Gợi ý theo diễn viên yêu thích

### Tên mô tả API
Gợi ý phim mới của diễn viên user thường xem, mua vé hoặc review tốt.

### API
```http
GET /api/v1/recommendations/favorite-actors?limit=10
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
      "actorId": 1,
      "actorName": "Favorite Star",
      "preferenceScore": 5.0,
      "movies": [
        {
          "movieId": 2,
          "title": "Action Movie New Release",
          "reasons": ["New movie from favorite actor: Favorite Star"]
        }
      ]
    }
  ]
}
```

## 5. Admin debug recommendation

### Tên mô tả API
Admin xem điểm profile, số interaction/booking/review và kết quả gợi ý của một user.

### API
```http
GET /api/v1/admin/recommendations/users/{{userId}}/debug?limit=10
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
    "userId": 2,
    "trailerInteractionCount": 1,
    "bookingHistoryCount": 0,
    "reviewCount": 0,
    "recommendations": [
      {
        "movieId": 2,
        "title": "Action Movie New Release",
        "score": 11.25
      }
    ]
  }
}
```
