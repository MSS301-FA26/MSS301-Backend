# Phase 3 - Movie, genre và actor management

Phase 3 theo mapping tương ứng phần quản lý phim, thể loại và diễn viên. Đây là
dữ liệu gốc cho showtime, booking, review, recommendation và upload poster/trailer.

## Quy tắc nghiệp vụ chính

- Admin tạo/sửa/xóa genre, actor và movie.
- Public chỉ xem phim không ở trạng thái `INACTIVE`.
- Public actor list chỉ trả actor đã có ít nhất một phim public liên kết.
- Movie status gồm `UPCOMING`, `NOW_SHOWING`, `ENDED`, `INACTIVE`.
- `actorIds` là toàn bộ diễn viên của phim.
- `mainActorIds` là diễn viên chính và phải là tập con của `actorIds`.
- Backend tự sinh `mainActors` từ `mainActorIds` và `castList` từ `actorIds`.
- Actor đang được gắn với movie không được xóa.
- Movie đã `NOW_SHOWING` không cho cập nhật metadata bằng PUT theo rule hiện tại.

## 1. Tạo thể loại

### Tên mô tả API
Admin tạo thể loại phim.

### API
```http
POST /api/v1/admin/genres
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Action Test {{$timestamp}}",
  "description": "Mô tả thể loại phải đủ dài theo validation hiện tại. Có thể ghi nội dung từ 200 đến 1000 ký tự để backend chấp nhận khi test API tạo thể loại trong CinemaAI."
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Action Test",
    "description": "Mô tả thể loại..."
  },
  "message": "Genre created successfully"
}
```

## 2. Cập nhật thể loại

### API
```http
PUT /api/v1/admin/genres/{{genreId}}
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Action Updated {{$timestamp}}",
  "description": "Mô tả thể loại sau cập nhật vẫn phải có độ dài hợp lệ, tối thiểu hai trăm ký tự và tối đa một nghìn ký tự theo validation của backend."
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Action Updated"
  },
  "message": "Genre updated successfully"
}
```

## 3. Danh sách thể loại public

### API
```http
GET /api/v1/genres
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
      "name": "Action"
    }
  ]
}
```

## 4. Tạo diễn viên

### Tên mô tả API
Admin tạo diễn viên trước khi gán vào phim.

### API
```http
POST /api/v1/admin/actors
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Bao Khanh {{$timestamp}}",
  "biography": "Diễn viên dùng để test phase 3.",
  "avatarUrl": "https://example.com/bao-khanh.jpg"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Bao Khanh",
    "movieCount": 0
  },
  "message": "Actor created successfully"
}
```

## 5. Load/Search diễn viên cho dropdown admin

### Tên mô tả API
FE gọi API này khi admin chọn diễn viên trong form tạo/cập nhật phim.

### API
```http
GET /api/v1/admin/actors?keyword=Bao&limit=20
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
      "name": "Bao Khanh",
      "movieCount": 0
    }
  ]
}
```

## 6. Cập nhật diễn viên

### API
```http
PUT /api/v1/admin/actors/{{actorId}}
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "name": "Bao Khanh Updated",
  "biography": "Thông tin diễn viên sau cập nhật.",
  "avatarUrl": "https://example.com/bao-khanh-updated.jpg"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Bao Khanh Updated"
  },
  "message": "Actor updated successfully"
}
```

## 7. Tạo phim

### Tên mô tả API
Admin tạo phim, gán thể loại qua `genreIds`, gán diễn viên qua `actorIds` và
gán diễn viên chính qua `mainActorIds`.

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
  "trailerUrl": "https://example.com/trailer.mp4",
  "posterUrl": "https://example.com/poster.jpg",
  "avatarUrl": "https://example.com/avatar.jpg",
  "durationMinutes": 120,
  "releaseDate": "2026-07-01",
  "language": "English",
  "subtitleLanguage": "Vietnamese",
  "status": "UPCOMING",
  "ageRating": "13+",
  "director": "Action Director",
  "genreIds": [{{genreId}}],
  "actorIds": [{{actorId}}],
  "mainActorIds": [{{actorId}}]
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Action Movie A",
    "status": "UPCOMING",
    "ageRating": "13+",
    "mainActors": "Bao Khanh",
    "castList": "Bao Khanh",
    "genres": [{"id": 1, "name": "Action"}],
    "actors": [{"id": 1, "name": "Bao Khanh", "movieCount": 1}],
    "mainActorIds": [1]
  },
  "message": "Movie created successfully"
}
```

## 8. Admin tìm phim

### API
```http
GET /api/v1/admin/movies?keyword=Action&status=UPCOMING&genreId={{genreId}}&page=0&size=20
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
    "items": [],
    "page": 0,
    "size": 20,
    "totalItems": 0,
    "totalPages": 0
  }
}
```

## 9. Public tìm phim

### Tên mô tả API
Khách tìm phim public. API không trả phim có trạng thái `INACTIVE`.

### API
```http
GET /api/v1/movies?keyword=Action&genreId={{genreId}}&page=0&size=20
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
    "items": [
      {
        "id": 1,
        "title": "Action Movie A",
        "status": "UPCOMING"
      }
    ],
    "page": 0,
    "size": 20
  }
}
```

## 10. Lấy chi tiết phim public

### API
```http
GET /api/v1/movies/{{movieId}}
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
    "title": "Action Movie A",
    "genres": [],
    "actors": []
  }
}
```

## 11. Cập nhật phim

### API
```http
PUT /api/v1/admin/movies/{{movieId}}
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "title": "Action Movie A Updated",
  "description": "Phim dùng để test API sau cập nhật.",
  "trailerUrl": "https://example.com/trailer-2.mp4",
  "posterUrl": "https://example.com/poster-2.jpg",
  "avatarUrl": "https://example.com/avatar-2.jpg",
  "durationMinutes": 125,
  "releaseDate": "2026-07-02",
  "language": "English",
  "subtitleLanguage": "Vietnamese",
  "status": "UPCOMING",
  "ageRating": "16+",
  "director": "Updated Director",
  "genreIds": [{{genreId}}],
  "actorIds": [{{actorId}}],
  "mainActorIds": [{{actorId}}]
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Action Movie A Updated",
    "mainActors": "Bao Khanh"
  },
  "message": "Movie updated successfully"
}
```

## 12. Đổi trạng thái phim

### API
```http
PATCH /api/v1/admin/movies/{{movieId}}/status
Authorization: Bearer {{adminToken}}
```

### JSON
```json
{
  "status": "NOW_SHOWING"
}
```

### Post-response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "NOW_SHOWING"
  },
  "message": "Movie status updated successfully"
}
```

## 13. Lấy phim theo diễn viên

### API
```http
GET /api/v1/actors/{{actorId}}/movies
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
      "title": "Action Movie A",
      "actors": [{"id": 1, "name": "Bao Khanh"}]
    }
  ]
}
```

## 14. Xóa mềm phim

### API
```http
DELETE /api/v1/admin/movies/{{movieId}}
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
  "data": null,
  "message": "Movie deleted successfully"
}
```

## Checklist hoàn tất Phase 3

- Admin CRUD genre hoạt động.
- Admin CRUD actor hoạt động, actor name tối đa 50 ký tự.
- Admin actor dropdown hỗ trợ `keyword` và `limit`.
- Admin tạo/cập nhật movie với `genreIds`, `actorIds`, `mainActorIds`.
- `mainActorIds` không nằm trong `actorIds` trả lỗi validation nghiệp vụ.
- Public movie search/detail không trả phim `INACTIVE`.
- Public actor search/detail chỉ hiển thị actor có movie public.
- Lấy phim theo actor trả đúng movie đã gán actor.
- Xóa movie là chuyển status `INACTIVE`.
