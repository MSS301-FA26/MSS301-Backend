# Phase 3 - Post-response Scripts

Dán phần **Post-response** vào tab **Scripts -> Post-response** trong Postman.

## 1. Tạo genre

### API
```http
POST /api/v1/admin/genres
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo genre thành công", function () {
  pm.expect(pm.response.code).to.eql(201);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.not.eql(undefined);
});

pm.collectionVariables.set("genreId", body.data.id);
pm.collectionVariables.set("genreName", body.data.name);
```

## 2. Cập nhật genre

### API
```http
PUT /api/v1/admin/genres/{{genreId}}
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Cập nhật genre thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.eql(Number(pm.collectionVariables.get("genreId")));
});

pm.collectionVariables.set("genreName", body.data.name);
```

## 3. Danh sách genre public

### API
```http
GET /api/v1/genres
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Danh sách genre public thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data).to.be.an("array");
});
```

## 4. Tạo actor

### API
```http
POST /api/v1/admin/actors
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo actor thành công", function () {
  pm.expect(pm.response.code).to.eql(201);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.movieCount).to.eql(0);
});

pm.collectionVariables.set("actorId", body.data.id);
pm.collectionVariables.set("actorName", body.data.name);
```

## 5. Load/Search actor cho dropdown

### API
```http
GET /api/v1/admin/actors?keyword={{actorName}}&limit=20
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();
const actorId = Number(pm.collectionVariables.get("actorId"));
const matchedActor = (body.data || []).find((actor) => actor.id === actorId) || body.data?.[0];

pm.test("Load/search actor dropdown thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data).to.be.an("array");
});

if (matchedActor?.id) {
  pm.collectionVariables.set("actorId", matchedActor.id);
  pm.collectionVariables.set("actorName", matchedActor.name);
}
```

## 6. Cập nhật actor

### API
```http
PUT /api/v1/admin/actors/{{actorId}}
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Cập nhật actor thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.eql(Number(pm.collectionVariables.get("actorId")));
});

pm.collectionVariables.set("actorName", body.data.name);
```

## 7. Tạo movie

### API
```http
POST /api/v1/admin/movies
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();
const actorId = Number(pm.collectionVariables.get("actorId"));
const genreId = Number(pm.collectionVariables.get("genreId"));
const actorIds = (body.data.actors || []).map((actor) => actor.id);
const genreIds = (body.data.genres || []).map((genre) => genre.id);

pm.test("Tạo movie thành công", function () {
  pm.expect(pm.response.code).to.eql(201);
  pm.expect(body.success).to.eql(true);
  pm.expect(actorIds).to.include(actorId);
  pm.expect(genreIds).to.include(genreId);
  pm.expect(body.data.mainActorIds).to.include(actorId);
});

pm.collectionVariables.set("movieId", body.data.id);
pm.collectionVariables.set("phase3MovieId", body.data.id);
pm.collectionVariables.set("movieTitle", body.data.title);
```

## 8. Admin tìm phim

### API
```http
GET /api/v1/admin/movies?keyword={{movieTitle}}&page=0&size=20
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();
const movieId = Number(pm.collectionVariables.get("movieId"));
const items = body.data?.items || [];
const matchedMovie = items.find((movie) => movie.id === movieId);

pm.test("Admin tìm phim thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(items).to.be.an("array");
  pm.expect(matchedMovie).to.not.eql(undefined);
});
```

## 9. Public tìm phim

### API
```http
GET /api/v1/movies?keyword={{movieTitle}}&genreId={{genreId}}&page=0&size=20
```

### Post-response
```javascript
const body = pm.response.json();
const items = body.data?.items || [];

pm.test("Public tìm phim thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(items).to.be.an("array");
});

if (items[0]?.id) {
  pm.collectionVariables.set("foundMovieId", items[0].id);
}
```

## 10. Chi tiết phim public

### API
```http
GET /api/v1/movies/{{movieId}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Chi tiết phim public thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.eql(Number(pm.collectionVariables.get("movieId")));
});
```

## 11. Cập nhật movie

### API
```http
PUT /api/v1/admin/movies/{{movieId}}
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Cập nhật movie thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.id).to.eql(Number(pm.collectionVariables.get("movieId")));
});

pm.collectionVariables.set("movieTitle", body.data.title);
```

## 12. Đổi trạng thái movie

### API
```http
PATCH /api/v1/admin/movies/{{movieId}}/status
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Đổi trạng thái movie thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data.status).to.not.be.empty;
});

pm.collectionVariables.set("movieStatus", body.data.status);
```

## 13. Lấy phim theo actor

### API
```http
GET /api/v1/actors/{{actorId}}/movies
```

### Post-response
```javascript
const body = pm.response.json();
const expectedMovieId = Number(pm.collectionVariables.get("phase3MovieId"));
const matchedMovie = (body.data || []).find((movie) => movie.id === expectedMovieId);

pm.test("Lấy phim theo actor thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(body.data).to.be.an("array");
  pm.expect(matchedMovie).to.not.eql(undefined);
});
```

## 14. Xóa mềm movie

### API
```http
DELETE /api/v1/admin/movies/{{movieId}}
Authorization: Bearer {{adminToken}}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Xóa mềm movie thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
});
```
