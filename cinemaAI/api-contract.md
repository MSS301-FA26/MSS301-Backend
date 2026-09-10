# CinemaAI API Contract

Nguồn: controller và DTO tại ngày 2026-06-25. Base URL local: `http://localhost:8080`.

## 1. Quy ước

- Base path: `/api/v1`.
- `Public`: không cần token.
- `Auth`: JWT hợp lệ.
- `Customer`: authenticated; nghiệp vụ giả định role customer.
- `Admin`: role `ADMIN`.
- `Staff`: role `STAFF` hoặc `ADMIN` theo SecurityConfig.
- Request body dùng JSON trừ upload ảnh (`multipart/form-data`) và VNPay callback (query string).
- Response thông thường: `ApiResponse<ResponseType>`.
- Response phân trang: `ApiResponse<PageResponse<T>>`.

Status chung:

| Status | Khi dùng |
|---|---|
| `200` | Thành công mặc định |
| `201` | Resource được tạo |
| `204` | Xóa không trả body |
| `302` | VNPay return redirect |
| `400` | Validation/body/state transition không hợp lệ |
| `401` | Thiếu/sai token hoặc login thất bại |
| `403` | Không đủ role/quyền |
| `404` | Resource không tồn tại |
| `405` | Sai HTTP method |
| `409` | Trùng dữ liệu hoặc conflict tài nguyên |
| `500` | Lỗi ngoài dự kiến |

## 2. Authentication

| Method + path | Access | Request | Response | Success |
|---|---|---|---|---|
| `POST /auth/register` | Public | `RegisterRequest` | `RegisterResponse` | `201` |
| `POST /auth/login` | Public | `LoginRequest` | `AuthResponse` | `200` |
| `POST /auth/google` | Public | `GoogleLoginRequest` | `AuthResponse` | `200` |
| `POST /auth/google/verify` | Public | `GoogleOtpVerifyRequest` | `AuthResponse` | `200` |
| `POST /auth/refresh` | Public | `RefreshTokenRequest` | `AuthResponse` | `200` |
| `POST /auth/logout` | Public | `LogoutRequest` | `Void` | `200` |
| `POST /auth/verify-email` | Public | `EmailVerificationRequest` | `Void` | `200` |
| `POST /auth/verify-email/request` | Public | `EmailOtpRequest` | `Void` | `200` |
| `POST /auth/password-reset/request` | Public | `PasswordResetRequest` | `TokenResponse` | `200` |
| `POST /auth/password-reset/confirm` | Public | `PasswordResetConfirmRequest` | `Void` | `200` |

## 3. Customer và public

### Profile và catalog

| Method + path | Access | Request | Response | Success |
|---|---|---|---|---|
| `GET /users/me` | Auth | — | `UserProfileResponse` | `200` |
| `PUT /users/me` | Auth | `UserProfileUpdateRequest` | `UserProfileResponse` | `200` |
| `POST /users/me/avatar` | Auth | multipart `file` | `UserProfileResponse` | `200` |
| `POST /users/me/password` | Auth | `ChangePasswordRequest` | `Void` | `200` |
| `GET /movies` | Public | query `keyword,status,genreId,fromDate,toDate,page,size` | `PageResponse<MovieResponse>` | `200` |
| `GET /movies/{movieId}` | Public | path | `MovieResponse` | `200` |
| `GET /genres` | Public | query `page,size` | `PageResponse<GenreResponse>` | `200` |
| `GET /genres/{genreId}` | Public | path | `GenreResponse` | `200` |
| `GET /actors` | Public | query `keyword,page,size` | `PageResponse<ActorResponse>` | `200` |
| `GET /actors/{actorId}` | Public | path | `ActorResponse` | `200` |
| `GET /actors/{actorId}/movies` | Public | path | `List<MovieResponse>` | `200` |
| `GET /cinema` hoặc `/cinemas` | Public | — | `CinemaResponse` | `200` |
| `GET /cinemas/{cinemaId}` | Public | path | `CinemaResponse` | `200` |
| `GET /cinema/rooms` | Public | — | `List<RoomResponse>` | `200` |
| `GET /cinemas/{cinemaId}/rooms` | Public | path | `List<RoomResponse>` | `200` |
| `GET /showtimes` | Public | query `movieId,roomId,date,page,size` | `PageResponse<ShowtimeResponse>` | `200` |
| `GET /showtimes/{showtimeId}` | Public | path | `ShowtimeResponse` | `200` |
| `GET /showtimes/{showtimeId}/seat-map` | Public | path | `ShowtimeSeatMapResponse` | `200` |
| `GET /foods/items` | Public | query `page,size` | `PageResponse<FoodItemResponse>` | `200` |
| `GET /foods/combos` | Public | query `page,size` | `PageResponse<FoodComboResponse>` | `200` |
| `GET /ticket-pricing/combos` | Public | — | `List<TicketComboResponse>` | `200` |
| `POST /ticket-pricing/validate` | Auth | `TicketPriceValidationRequest` | `TicketPriceValidationResponse` | `200` |

### Booking và payment

| Method + path | Access | Request | Response | Success |
|---|---|---|---|---|
| `POST /bookings/hold` | Auth | `HoldSeatsRequest` | `BookingResponse` | `201` |
| `POST /bookings` | Auth | `CreateBookingRequest` | `BookingResponse` | `201` |
| `GET /bookings` | Auth | query `status,page,size` | `PageResponse<BookingResponse>` | `200` |
| `GET /bookings/{bookingId}` | Auth/owner | path | `BookingResponse` | `200` |
| `DELETE /bookings/{bookingId}` | Auth/owner | path | `BookingResponse` | `200` |
| `POST /payments/vnpay/create` | Auth/owner | `VNPayPaymentRequest` | `PaymentResponse` | `200` |
| `GET /payments/vnpay/return` | Public | VNPay query params | redirect, no body | `302` |
| `GET /payments/vnpay/ipn` | Public | VNPay query params | provider result string | `200` |
| `POST /payments/mock` | Auth/owner | query `bookingId` | `PaymentResponse` | `200` |
| `GET /payments/booking/{bookingId}` | Auth/owner | path | `PaymentResponse` | `200` |

### Wishlist, loyalty, review, recommendation, notification

| Method + path | Access | Request | Response | Success |
|---|---|---|---|---|
| `POST /wishlist` | Auth | `WishlistCreateRequest` | `WishlistResponse` | `201` |
| `GET /wishlist` | Auth | — | `List<WishlistResponse>` | `200` |
| `DELETE /wishlist/{movieId}` | Auth | path | — | `204` |
| `GET /loyalty/me` | Auth | — | `LoyaltyResponse` | `200` |
| `GET /loyalty/config` | Auth | — | `LoyaltyConfigurationResponse` | `200` |
| `POST /loyalty/me/redeem` | Auth | query `points` | `LoyaltyResponse` | `200` |
| `GET /wallet/me` | Auth | — | `WalletResponse` | `200` |
| `GET /reviews/movies/{movieId}` | Public | query `page,size` | `PageResponse<ReviewResponse>` | `200` |
| `GET /reviews/movies/{movieId}/average-rating` | Public | path | `Double` | `200` |
| `POST /reviews/movies/{movieId}` | Customer | `ReviewRequest` | `ReviewResponse` | `201` |
| `PUT /reviews/{reviewId}` | Customer/owner | `ReviewRequest` | `ReviewResponse` | `200` |
| `DELETE /reviews/{reviewId}` | Customer/owner | path | `Void` | `200` |
| `POST /recommendations/trailer-interactions` | Auth | `TrailerInteractionRequest` | `TrailerInteractionResponse` | `200` |
| `POST /recommendations/preferences/refresh` | Auth | — | `UserPreferenceProfileResponse` | `200` |
| `GET /recommendations/preferences/me` | Auth | — | `UserPreferenceProfileResponse` | `200` |
| `GET /recommendations/movies` | Auth | query `limit` | `List<MovieRecommendationResponse>` | `200` |
| `GET /recommendations/favorite-actors` | Auth | query `limit` | `List<FavoriteActorRecommendationResponse>` | `200` |
| `POST /notifications` | Admin | `NotificationCreateRequest` | `NotificationResponse` | `201` |
| `GET /notifications/me` | Auth | — | `List<NotificationResponse>` | `200` |
| `GET /notifications/me/unread` | Auth | — | `List<NotificationResponse>` | `200` |
| `PATCH /notifications/{id}/read` | Auth/owner | path | `NotificationResponse` | `200` |
| `PATCH /notifications/me/read-all` | Auth | — | `Integer` số bản ghi | `200` |

## 4. Staff check-in

Hai alias dùng cùng contract:

| Method + path | Access | Request | Response | Success |
|---|---|---|---|---|
| `POST /staff/check-in` | Staff/Admin | `CheckInRequest` | `BookingResponse` | `200` |
| `POST /staff/showtimes/{showtimeId}/cancel` | Staff/Admin | query `reason` | `ShowtimeResponse` | `200` |
| `GET /staff/bulk-refunds/failed` | Staff/Admin | query `page,size` | `PageResponse<BulkRefundDetailResponse>` | `200` |
| `GET /staff/bulk-refunds/{bookingId}/detail` | Staff/Admin | path | `BulkRefundDetailResponse` | `200` |
| `POST /staff/bulk-refunds/{bookingId}/confirm` | Staff/Admin | `ConfirmManualRefundRequest` | `BulkRefundDetailResponse` | `200` |
| `POST /admin/check-in` | Admin | `CheckInRequest` | `BookingResponse` | `200` |

## 5. Admin

Tất cả endpoint dưới đây có prefix `/api/v1/admin` và yêu cầu role `ADMIN`.

### Actor, genre, movie

| Method + path | Request | Response | Success |
|---|---|---|---|
| `GET /actors` | query `keyword,page,size` | `PageResponse<ActorResponse>` | `200` |
| `POST /actors` | `ActorRequest` | `ActorResponse` | `201` |
| `PUT /actors/{actorId}` | `ActorRequest` | `ActorResponse` | `200` |
| `DELETE /actors/{actorId}` | path | `Void` | `200` |
| `POST /genres` | `GenreRequest` | `GenreResponse` | `201` |
| `PUT /genres/{genreId}` | `GenreRequest` | `GenreResponse` | `200` |
| `DELETE /genres/{genreId}` | path | `Void` | `200` |
| `GET /movies` | query `keyword,status,genreId,fromDate,toDate,page,size` | `PageResponse<MovieResponse>` | `200` |
| `GET /movies/{movieId}` | path | `MovieResponse` | `200` |
| `POST /movies` | `MovieCreateRequest` | `MovieResponse` | `201` |
| `PUT /movies/{movieId}` | `MovieUpdateRequest` | `MovieResponse` | `200` |
| `PATCH /movies/{movieId}/status` | `MovieStatusUpdateRequest` | `MovieResponse` | `200` |
| `DELETE /movies/{movieId}` | path | `Void` | `200` |

### Cinema, room, seat, showtime

| Method + path | Request | Response | Success |
|---|---|---|---|
| `GET /cinema` | — | `CinemaResponse` | `200` |
| `POST /cinema` | `CinemaRequest` | `CinemaResponse` | `201` |
| `PUT /cinema` | `CinemaRequest` | `CinemaResponse` | `200` |
| `PATCH /cinema/status` | query `status` | `CinemaResponse` | `200` |
| `DELETE /cinema` | — | `Void` | `200` |
| `GET /rooms` | — | `List<RoomResponse>` | `200` |
| `GET /rooms/{roomId}` | path | `RoomResponse` | `200` |
| `GET /rooms/{roomId}/seats` | path | `List<SeatResponse>` | `200` |
| `GET /rooms/seats/{seatId}` | path | `SeatResponse` | `200` |
| `POST /rooms` | `RoomRequest` | `RoomResponse` | `201` |
| `PUT /rooms/{roomId}` | `RoomRequest` | `RoomResponse` | `200` |
| `PATCH /rooms/{roomId}/status` | query `status` | `RoomResponse` | `200` |
| `POST /rooms/{roomId}/seats/generate` | `SeatRowGenerationRequest` | `List<SeatResponse>` | `201` |
| `PUT /rooms/{roomId}/seats` | `SeatLayoutRequest` | `List<SeatResponse>` | `200` |
| `PUT /rooms/seats/{seatId}` | `SeatUpdateRequest` | `SeatResponse` | `200` |
| `DELETE /rooms/seats/{seatId}` | path | `SeatResponse` | `200` |
| `GET /showtimes` | query `movieId,roomId,cinemaId,status,date,page,size` | `PageResponse<ShowtimeResponse>` | `200` |
| `GET /showtimes/{showtimeId}` | path | `ShowtimeResponse` | `200` |
| `GET /showtimes/{showtimeId}/seat-map` | path | `ShowtimeSeatMapResponse` | `200` |
| `POST /showtimes` | `ShowtimeRequest` | `ShowtimeResponse` | `201` |
| `POST /showtimes/bulk` | `BulkShowtimeRequest` | `List<ShowtimeResponse>` | `201` |
| `PUT /showtimes/{showtimeId}` | `ShowtimeRequest` | `ShowtimeResponse` | `200` |
| `PATCH /showtimes/{showtimeId}/status` | query `status`, optional `reason` when cancelling | `ShowtimeResponse` | `200` |
| `POST /showtimes/{showtimeId}/cancel` | query `reason` | `ShowtimeResponse` | `200` |
| `POST /showtimes/{showtimeId}/cancel-and-refund` | `CancelShowtimeRequest` | `BulkRefundResponse` | `200` |
| `DELETE /showtimes/{showtimeId}` | path | — | `204` |

### Bulk refund follow-up

| Method + path | Request | Response | Success |
|---|---|---|---|
| `GET /bulk-refunds/failed` | query `page,size` | `PageResponse<BulkRefundDetailResponse>` | `200` |
| `GET /bulk-refunds/{bookingId}/detail` | path | `BulkRefundDetailResponse` | `200` |
| `POST /bulk-refunds/{bookingId}/confirm` | `ConfirmManualRefundRequest` | `BulkRefundDetailResponse` | `200` |

### Food và ticket pricing

| Method + path | Request | Response | Success |
|---|---|---|---|
| `GET /foods/items` | query `page,size` | `PageResponse<FoodItemResponse>` | `200` |
| `GET /foods/combos` | query `page,size` | `PageResponse<FoodComboResponse>` | `200` |
| `POST /foods/items` | `FoodItemRequest` | `FoodItemResponse` | `201` |
| `POST /foods/combos` | `FoodComboRequest` | `FoodComboResponse` | `201` |
| `PUT /foods/items/{itemId}` | `FoodItemRequest` | `FoodItemResponse` | `200` |
| `PUT /foods/combos/{comboId}` | `FoodComboRequest` | `FoodComboResponse` | `200` |
| `DELETE /foods/items/{itemId}` | path | `FoodItemResponse` | `200` |
| `DELETE /foods/combos/{comboId}` | path | `FoodComboResponse` | `200` |
| `GET /ticket-pricing/rules` | filters + paging | `PageResponse<TicketPricingRuleResponse>` | `200` |
| `POST /ticket-pricing/rules` | `TicketPricingRuleRequest` | `TicketPricingRuleResponse` | `201` |
| `PUT /ticket-pricing/rules/{ruleId}` | `TicketPricingRuleRequest` | `TicketPricingRuleResponse` | `200` |
| `DELETE /ticket-pricing/rules/{ruleId}` | path | `Void` | `200` |
| `GET /ticket-pricing/combos` | query `active,keyword,page,size` | `PageResponse<TicketComboResponse>` | `200` |
| `POST /ticket-pricing/combos` | `TicketComboRequest` | `TicketComboResponse` | `201` |
| `PUT /ticket-pricing/combos/{comboId}` | `TicketComboRequest` | `TicketComboResponse` | `200` |
| `DELETE /ticket-pricing/combos/{comboId}` | path | `Void` | `200` |

Ticket-pricing admin endpoints đang `@Hidden` trong Swagger nhưng vẫn tồn tại.

### Booking, user, loyalty

| Method + path | Request | Response | Success |
|---|---|---|---|
| `POST /food-orders` | `FoodOrderRequest` | `FoodOrderResponse` (`bookingId`/`bookingCode` are `null`) | `201` |
| `GET /food-orders/my` | — | `List<FoodOrderResponse>` | `200` |
| `DELETE /food-orders/{foodOrderId}` | path | `FoodOrderResponse` | `200` |
| `POST /bookings/{bookingId}/food-orders` | `FoodOrderRequest` | `FoodOrderResponse` | `200` |
| `GET /bookings/{bookingId}/food-orders` | path | `List<FoodOrderResponse>` | `200` |
| `POST /payments/food-orders/{foodOrderId}/vnpay/create` | path | `PaymentResponse` | `200` |
| `POST /payments/food-orders/{foodOrderId}/mock` | path | `PaymentResponse` | `200` |

Standalone pickup QR (STAFF/ADMIN):

| Endpoint | Input | Output | Success |
|---|---|---|---|
| `GET /staff/check-in/food-orders/lookup` | query `code` (order code or `CINEAI:FOOD:...`) | `FoodOrderResponse` | `200` |
| `POST /staff/check-in/food-orders/pickup` | `{ "code": "CINEAI:FOOD:..." }` | `FoodOrderResponse` with `PICKED_UP` | `200` |

For a paid standalone order, `FoodOrderResponse.qrCode` contains a one-time pickup QR. After pickup,
`status` is `PICKED_UP`, `pickedUpAt` is populated, and `qrCode` is no longer returned. A repeated pickup
request returns `409 Conflict`.

Standalone food orders use a 15-minute `PENDING_PAYMENT` window. `FoodOrderResponse.expiresAt`
is the source of truth for the customer countdown and is also sent to VNPay as `vnp_ExpireDate`.
Cancelling or failing a VNPay attempt does not cancel the order; the customer may retry until
`expiresAt`. Customer cancellation changes the order to `CANCELLED`; automatic timeout changes it
to `EXPIRED`.
| `GET /bookings` | query `status,page,size` | `PageResponse<BookingResponse>` | `200` |
| `GET /bookings/{bookingId}` | path | `BookingResponse` | `200` |
| `DELETE /bookings/{bookingId}` | path | `BookingResponse` | `200` |
| `POST /bookings/{bookingId}/check-in` | optional `CheckInRequest` | `BookingResponse` | `200` |
| `GET /users` | — | `List<UserProfileResponse>` | `200` |
| `GET /users/{userId}` | path | `UserProfileResponse` | `200` |
| `POST /users/staff` | `AdminStaffCreateRequest` | `UserProfileResponse` | `200` |
| `PATCH /users/{userId}/status` | `AdminUserStatusUpdateRequest` | `UserProfileResponse` | `200` |
| `GET /loyalty/config` | — | `LoyaltyConfigurationResponse` | `200` |
| `PUT /loyalty/config` | `LoyaltyConfigurationRequest` | `LoyaltyConfigurationResponse` | `200` |
| `GET /loyalty/transactions` | query `keyword,from,to,page,size` | `PageResponse<LoyaltyTransactionResponse>` | `200` |
| `GET /loyalty/report` | query `from,to` | `LoyaltyReportResponse` | `200` |
| `POST /loyalty/expire-now` | — | `Integer` | `200` |
| `POST /loyalty/add` | `LoyaltyAddRequest` | `LoyaltyResponse` | `200` |
| `POST /loyalty/{userId}/redeem` | query `points` | `LoyaltyResponse` | `200` |

### Reports, moderation, debug, upload

| Method + path | Request | Response | Success |
|---|---|---|---|
| `GET /reports/revenue` | query `from,to` | `RevenueReportResponse` | `200` |
| `GET /reports/top-movies` | query `from,to,limit` | `List<TopMovieResponse>` | `200` |
| `GET /reports/occupancy` | query `from,to` | `List<RoomOccupancyResponse>` | `200` |
| `GET /reviews` | query `movieId,status,page,size` | `PageResponse<ReviewResponse>` | `200` |
| `PATCH /reviews/{reviewId}/hide` | path | `ReviewResponse` | `200` |
| `DELETE /reviews/{reviewId}` | path | `Void` | `200` |
| `GET /recommendations/users/{userId}/debug` | query `limit` | `RecommendationDebugResponse` | `200` |
| `POST /uploads/images` | multipart `file`, query `folder` | `UploadedFileResponse` | `201` |

## 6. Contract maintenance rule

Khi endpoint thay đổi, cập nhật đồng thời:

1. controller và DTO;
2. `SecurityConfig`/`@PreAuthorize`;
3. OpenAPI annotation;
4. integration hoặc endpoint inventory test;
5. Postman collection nếu flow bị ảnh hưởng;
6. file này.

