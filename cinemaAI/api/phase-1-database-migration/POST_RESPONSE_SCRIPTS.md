# Phase 1 - Post-response Scripts

Dán phần **Post-response** vào tab **Scripts -> Post-response** trong Postman.

## 1. Kiểm tra genre seed

### Tên mô tả API
Kiểm tra schema và seed genre qua endpoint public.

### API
```http
GET /api/v1/genres
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();
const genres = body.data || [];
const action = genres.find((genre) => genre.name === "Action");

pm.test("Danh sách genre trả về thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(genres).to.be.an("array");
});

pm.test("Seed genre cơ bản tồn tại", function () {
  pm.expect(genres.length).to.be.greaterThan(0);
  pm.expect(action).to.not.eql(undefined);
});

if (action?.id) {
  pm.collectionVariables.set("genreId", action.id);
}
```

## 2. Kiểm tra movie seed

### Tên mô tả API
Kiểm tra schema movie, movie pagination và dữ liệu movie seed.

### API
```http
GET /api/v1/movies?size=10
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();
const page = body.data || {};
const movies = page.items || [];
const seededMovie = movies.find((movie) => movie.title === "The Last Orbit");

pm.test("Danh sách movie trả về thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(movies).to.be.an("array");
});

pm.test("Movie response có pagination contract", function () {
  pm.expect(page).to.have.property("page");
  pm.expect(page).to.have.property("size");
  pm.expect(page).to.have.property("totalItems");
  pm.expect(page).to.have.property("totalPages");
});

if (seededMovie?.id) {
  pm.collectionVariables.set("movieId", seededMovie.id);
  pm.collectionVariables.set("movieTitle", seededMovie.title);
}
```

## 3. Kiểm tra cinema/room seed

### Tên mô tả API
Kiểm tra seed rạp/phòng để dùng cho showtime và booking.

### API
```http
GET /api/v1/cinema/rooms
```

### JSON
```json
{}
```

### Post-response
```javascript
const body = pm.response.json();
const rooms = body.data || [];
const roomA = rooms.find((room) => room.name === "Room A");

pm.test("Danh sách room trả về thành công", function () {
  pm.expect(pm.response.code).to.eql(200);
  pm.expect(body.success).to.eql(true);
  pm.expect(rooms).to.be.an("array");
});

pm.test("Room seed có layout cơ bản", function () {
  pm.expect(roomA).to.not.eql(undefined);
  pm.expect(roomA.rowCount).to.be.greaterThan(0);
  pm.expect(roomA.columnCount).to.be.greaterThan(0);
});

if (roomA?.id) {
  pm.collectionVariables.set("roomId", roomA.id);
}
```

## 4. Tạo genre test

### Tên mô tả API
Tạo genre để kiểm tra schema write path và unique constraint.

### API
```http
POST /api/v1/admin/genres
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Action Test {{$timestamp}}",
  "description": "Thể loại phim hành động dùng để test dữ liệu seed và kiểm tra schema database trong CinemaAI. Mô tả này cố ý dài hơn hai trăm ký tự để đáp ứng validation hiện tại của backend, đồng thời giúp tester không gặp lỗi bad request khi tạo genre bằng Postman."
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo genre thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.test("Genre response có id và name", function () {
  pm.expect(body.data?.id).to.not.eql(undefined);
  pm.expect(body.data?.name).to.include("Action Test");
});

if (body.data?.id) {
  pm.collectionVariables.set("genreId", body.data.id);
}

if (body.data?.name) {
  pm.collectionVariables.set("genreName", body.data.name);
}
```

## 5. Tạo movie test

### Tên mô tả API
Tạo movie để kiểm tra schema movie cơ bản.

### API
```http
POST /api/v1/admin/movies
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "title": "Action Movie A {{$timestamp}}",
  "description": "Phim dùng để test API.",
  "durationMinutes": 120,
  "status": "NOW_SHOWING",
  "ageRating": "13+",
  "director": "Director Test",
  "mainActors": "Actor Test",
  "castList": "Actor Test, Supporting Actor"
}
```

### Post-response
```javascript
const body = pm.response.json();

pm.test("Tạo movie thành công", function () {
  pm.expect(pm.response.code).to.be.oneOf([200, 201]);
  pm.expect(body.success).to.eql(true);
});

pm.test("Movie response có id và title", function () {
  pm.expect(body.data?.id).to.not.eql(undefined);
  pm.expect(body.data?.title).to.include("Action Movie A");
});

if (body.data?.id) {
  pm.collectionVariables.set("movieId", body.data.id);
}

if (body.data?.title) {
  pm.collectionVariables.set("movieTitle", body.data.title);
}
```
