# Ghi chu test API CinemaAI theo phase mapping moi

Thu muc nay chia API theo tung phase de test nhanh trong Postman, Swagger hoac REST client.

Moi phase co file `README.md` theo cau truc:

- Ten mo ta API
- API
- JSON
- Post-response

Moi phase cung co file `POST_RESPONSE_SCRIPTS.md` de copy script vao tab **Scripts -> Post-response** trong Postman.

## Danh sach phase

- [Phase 0 - Nen tang dung chung](./phase-0-shared-foundation/README.md)
- [Phase 1 - Database migration/schema](./phase-1-database-migration/README.md)
- [Phase 2 - Auth, user va security](./phase-2-auth-user-security/README.md)
- [Phase 3 - Phim, the loai va dien vien](./phase-3-movie-genre-actor/README.md)
- [Phase 4 - Rap, phong, ghe va suat chieu](./phase-4-cinema-room-seat-showtime/README.md)
- [Phase 5 - Booking, khoa ghe, F&B va QR ve](./phase-5-booking-seat-locking-food-ticket-qr/README.md)
- [Phase 6 - Payment](./phase-6-payment/README.md)
- [Legacy - Booking, khoa ghe, F&B va QR ve theo numbering cu](./phase-6-booking-food-qr/README.md)
- [Legacy - AI goi y phim ca nhan hoa](./phase-4-ai-personalized-recommendation/README.md)
- [Legacy - Mot rap, phong chieu, ghe, suat chieu va gia ve](./phase-5-single-cinema-showtime/README.md)
- [Post-response Scripts tong hop cho Postman](./post-response-scripts/README.md)
- [Collection Post-response Script](./COLLECTION_POST_RESPONSE_SCRIPT.md)

## Script Post-response

Co 2 noi de lay script Postman:

- Trong tung phase: mo `POST_RESPONSE_SCRIPTS.md` cua phase dang test.
- Ban tong hop: mo folder `post-response-scripts`.

Dan script vao tab **Scripts -> Post-response** cua request tuong ung de tu luu `accessToken`,
`refreshToken`, `cinemaId`, `roomId`, `seatId`, `showtimeId`, `bookingId` va cac bien test khac
vao **Collection Variables**.

Coding standard cho Postman trong folder nay:

- Dung collection variables, khong dung environment variables.
- Dung camelCase: `accessToken`, `adminToken`, `movieId`, `genreId`, `bookingId`.
- API can token dung `Authorization: Bearer {{accessToken}}` hoac `Authorization: Bearer {{adminToken}}`.
- API phu thuoc du lieu truoc do dung bien trong URL/body, vi du `/api/v1/admin/movies/{{movieId}}`.

## Bien can thay khi test

- `{{adminToken}}`: token lay tu `POST /api/v1/auth/login` bang user co role `ADMIN`.
- `{{accessToken}}`: token lay tu `POST /api/v1/auth/login` bang user co role `CUSTOMER`.
- `{{staffToken}}`: token lay tu `POST /api/v1/auth/login` bang user co role `STAFF`.
- Cac id nhu `movieId`, `genreId`, `actorId`, `showtimeId`, `seatId`, `bookingId` can thay bang id that lay tu response truoc do.

## Goi y thu tu test nhanh

1. Phase 2: dang nhap de lay `{{adminToken}}`, `{{accessToken}}` hoac `{{staffToken}}`.
2. Phase 3: tao genre, actor va movie.
3. Phase 4: tao rap, phong, ghe va suat chieu.
4. Phase 5: giu ghe va tao booking `PENDING_PAYMENT`.
5. Phase 6: thanh toan, lay QR, refund neu can.
6. Legacy recommendation: ghi nhan trailer interaction va xem recommendation.
