# Phase 4 - Post-response Scripts

Dán phần **Post-response** vào tab **Scripts -> Post-response** trong Postman.

## 1. Ghi nhận trailer interaction

### Tên mô tả API
Lưu hành vi user xem trailer để làm tín hiệu recommendation.

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
  "watchedSeconds": 95,
  "totalSeconds": 100
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Lưu trailer interaction thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.collectionVariables.set("trailerInteractionId", body.data.id);

if (body.data.movieId) {
  pm.collectionVariables.set("movieId", body.data.movieId);
}
```

## 2. Refresh preference profile

### Tên mô tả API
Yêu cầu backend phân tích lại profile sở thích của user.

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
```javascript
const body = pm.response.json();

pm.test("Refresh preference profile thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

if (body.data?.userId) {
  pm.collectionVariables.set("recommendationUserId", body.data.userId);
}
```

## 3. Lấy danh sách phim recommend

### Tên mô tả API
Lấy danh sách phim hệ thống recommend cho user.

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
```javascript
const body = pm.response.json();

pm.test("Lấy recommendation thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

if (body.data?.[0]?.movieId) {
  pm.collectionVariables.set("recommendedMovieId", body.data[0].movieId);
}
```

## 4. Lấy favorite actors

### Tên mô tả API
Lấy danh sách diễn viên hệ thống đánh giá là user yêu thích.

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
```javascript
const body = pm.response.json();

pm.test("Lấy favorite actors thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});

if (body.data?.[0]?.actorId) {
  pm.collectionVariables.set("favoriteActorId", body.data[0].actorId);
}
```
