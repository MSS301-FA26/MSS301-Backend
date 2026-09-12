# CinemaAI — Kế hoạch phát triển hai tháng

Thời gian dự kiến: **16/09–11/11**. Kế hoạch bắt đầu sau khi giảng viên kiểm tra Conceptual và Stitch ngày 15/09. Nếu giảng viên yêu cầu thay đổi service boundary, leader cập nhật kế hoạch trong ngày 16/09 trước khi nhóm code.

## 1. Mục tiêu cuối kỳ

Nhóm cần hoàn thiện một luồng chạy xuyên suốt trên Web và Flutter:

```text
Đăng nhập → Xem phim → Chọn suất chiếu → Giữ ghế 3 phút
→ Chọn vé/đồ ăn → Thanh toán → Nhận QR → Xem lịch sử
→ Phát interaction/event → Nhận recommendation
```

Kiến trúc mục tiêu:

- API Gateway/BFF.
- Identity Service.
- Catalog Service.
- Booking Service.
- Payment Service.
- Recommendation Service.
- PostgreSQL theo quyền sở hữu của từng service.
- REST cho xử lý cần kết quả ngay.
- RabbitMQ cho domain event.
- Web hoàn thiện theo Stitch.
- Mobile Flutter hoàn thiện core customer flow.
- Paper recommendation là bonus, không được làm trễ sản phẩm chính.

## 2. Phân công cố định

| Người | Phần sở hữu chính | Phần hỗ trợ |
|---|---|---|
| Người 1 — Leader | Architecture, Gateway, integration, Docker, event platform | Hỗ trợ Payment/Outbox và xử lý blocker |
| Người 2 | Identity và Catalog backend | Checkout quote, Catalog event |
| Người 3 | Booking và Payment backend | Seat concurrency, snapshot, refund |
| Người 4 | Stitch Web và Web frontend | Kiểm tra design system cho Flutter |
| Người 5 | Stitch Mobile và Flutter | Kiểm tra UI Web, test API customer flow |
| Người 6 | Recommendation và paper | Interaction/event contract, recommendation API |

Owner chịu trách nhiệm phần của mình chạy được, có test/tài liệu và tích hợp được; không chỉ chịu trách nhiệm viết code.

## 3. Milestone chung

| Mốc | Kết quả bắt buộc |
|---|---|
| 16/09 | Cập nhật thay đổi theo nhận xét của giảng viên, khóa scope |
| 22/09 | API/event contract và môi trường local chung |
| 29/09 | Foundation hoàn thành; Web/Flutter chạy với API thật hoặc mock đúng contract |
| 06/10 | Catalog, seat hold và booking draft chạy được |
| 13/10 | Demo end-to-end từ đăng nhập đến QR ticket |
| 20/10 | Recommendation tích hợp, admin/customer feature chính hoàn thành |
| 27/10 | Feature freeze; paper có kết quả thực nghiệm đầu tiên |
| 03/11 | Release candidate |
| 08/11 | Code/schema/design freeze |
| 11/11 | Hoàn thành đồ án |

## 4. Giai đoạn 1 — Khóa scope và dựng nền tảng

Thời gian: **16/09–29/09**.

### Người 1 — Leader

Hoàn thành trước 18/09:

- Tổng hợp nhận xét của giảng viên thành task.
- Khóa service boundary và entity ownership.
- Chốt API response/error format.
- Chốt event envelope: `eventId`, `eventType`, `version`, `occurredAt`, `aggregateId`, `correlationId`, `payload`.
- Quy định branch, pull request, reviewer và Definition of Done.

Hoàn thành trước 22/09:

- Tạo cấu trúc service/repository.
- Dựng Docker Compose cho PostgreSQL và RabbitMQ.
- Tạo API Gateway route mẫu.
- Tạo health check và correlation ID.
- Viết hướng dẫn chạy local ngắn.

Hoàn thành trước 29/09:

- Gateway route được Identity và Catalog.
- Authentication token được forward đúng.
- Có môi trường integration dùng chung.
- Review database ownership của Người 2 và 3.

### Người 2 — Identity và Catalog

Hoàn thành trước 20/09:

- Tách danh sách entity Identity/Catalog khỏi monolith.
- Chuẩn hóa migration PostgreSQL cho phạm vi được giao.
- Chốt OpenAPI cho Auth, Movie, Showtime, Seat, Pricing và Food.

Hoàn thành trước 25/09:

- Register, login, refresh token và profile.
- Movie list/detail, search/filter.
- Showtime list/detail.
- Room/seat definition read API.
- Food item/combo read API.

Hoàn thành trước 29/09:

- Ticket pricing read API.
- Unit test cho Auth/Catalog core API.
- Cung cấp mock/seed data cho Web, Flutter và Recommendation.

### Người 3 — Booking và Payment

Hoàn thành trước 20/09:

- Thiết kế database Booking/Payment theo conceptual đã duyệt.
- Thay quan hệ xuyên service bằng logical ID trong phần code mới.
- Chốt request/response cho hold, checkout, payment và booking history.

Hoàn thành trước 25/09:

- API giữ và giải phóng ghế.
- `holdExpiresAt = createdAt + 3 phút`.
- Job chuyển booking hết hạn sang `EXPIRED` và giải phóng ghế.
- Kiểm tra ghế thuộc đúng room/showtime.

Hoàn thành trước 29/09:

- Booking draft.
- Booking state machine.
- Concurrency test: hai user không giữ được cùng một ghế.
- Payment mock contract để Web/Flutter tích hợp sớm.

### Người 4 — Web

Hoàn thành trước 20/09:

- Khóa design system theo Stitch.
- Chuyển component thành code dùng lại được.
- Tạo API client và error handling chung.

Hoàn thành trước 25/09:

- Login/register.
- Home.
- Movie list, search/filter và movie detail.
- Showtime selection.

Hoàn thành trước 29/09:

- Seat selection UI.
- Food selection UI.
- Loading, empty và error states.
- Chạy được bằng API thật hoặc mock đúng OpenAPI contract.

### Người 5 — Flutter

Hoàn thành trước 20/09:

- Khởi tạo Flutter project.
- Thiết lập Riverpod, GoRouter, Dio và secure storage.
- Chuyển design token từ Stitch sang Flutter theme.
- Tạo API client và error model chung.

Hoàn thành trước 25/09:

- Login/register.
- Home.
- Movie list/search.
- Movie detail.

Hoàn thành trước 29/09:

- Showtime selection.
- Seat map component.
- Navigation core flow.
- Chạy được bằng API thật hoặc mock đúng contract.

### Người 6 — Recommendation và paper

Hoàn thành trước 20/09:

- Chốt interaction schema và event mapping.
- Chuẩn bị dataset pipeline.
- Chốt cách biểu diễn movie feature.
- Tạo outline paper.

Hoàn thành trước 25/09:

- Popularity baseline.
- Content-based baseline bằng movie metadata/embedding.
- Endpoint recommendation thử nghiệm.

Hoàn thành trước 29/09:

- Evaluation pipeline cơ bản.
- Time-based train/test split.
- Có kết quả baseline đầu tiên trên dữ liệu hiện có hoặc dataset thay thế đã ghi rõ nguồn.

## 5. Giai đoạn 2 — Hoàn thành luồng booking end-to-end

Thời gian: **30/09–13/10**.

### Người 1 — Leader

- Hoàn thiện Gateway routing và authorization.
- Dựng RabbitMQ exchange/queue convention.
- Tạo mẫu Transactional Outbox và idempotent consumer.
- Thiết lập integration test environment.
- Tổ chức integration checkpoint ngày 06/10 và 12/10.
- Hỗ trợ Người 3 tích hợp Payment event.

### Người 2 — Identity và Catalog

Hoàn thành trước 06/10:

- Ticket pricing đầy đủ.
- Kiểm tra movie/showtime/room/seat/food đang active.
- Batch checkout quote API.

Checkout quote phải trả:

- `quoteId`.
- `priceVersion`.
- `validUntil`.
- Showtime/movie/room/seat data cần snapshot.
- Ticket/food price.
- Subtotal, discount và total.

Hoàn thành trước 13/10:

- Event `MoviePublished`, `MovieUpdated`, `ShowtimeCancelled`, `FoodPriceChanged`.
- Integration test checkout quote.
- Fix contract sau lần tích hợp với Booking.

### Người 3 — Booking và Payment

Hoàn thành trước 06/10:

- Booking gọi batch checkout quote.
- Backend tự tính và xác nhận tổng tiền.
- Lưu snapshot movie/showtime/room/seat/ticket/food.
- Chuyển `HOLDING → PENDING_PAYMENT`.

Hoàn thành trước 13/10:

- Payment mock/VNPay development flow.
- Xác minh callback, amount và trạng thái booking.
- Callback idempotent.
- Payment success chuyển Booking sang `PAID`, Seat sang `BOOKED`.
- Sinh QR.
- Booking history/detail.
- Event `PaymentSucceeded`, `PaymentFailed`, `BookingPaid`, `BookingExpired`.

### Người 4 — Web

- Seat hold countdown 3 phút.
- Chọn ticket type và food/combo.
- Checkout hiển thị subtotal/discount/total từ backend.
- Payment redirect/result.
- Booking history/detail.
- QR ticket.
- Xử lý ghế vừa bị người khác giữ, hold hết hạn và payment failed.

### Người 5 — Flutter

- Seat selection và countdown 3 phút.
- Ticket/food selection.
- Checkout.
- Payment result.
- Booking history/detail.
- QR ticket.
- Xử lý token hết hạn, hold hết hạn, conflict và lỗi mạng.

### Người 6 — Recommendation và paper

- Collaborative filtering bằng BPR hoặc LightFM.
- Xây implicit feedback weights.
- Xử lý user/item cold-start.
- Fixed hybrid baseline.
- Adaptive hybrid phiên bản đầu.
- Consumer thử nghiệm cho `BookingPaid` và Catalog events.

Mốc ngày 13/10 chỉ đạt khi Web và Flutter đều chạy được core flow với backend tích hợp, không chỉ chạy mock.

## 6. Giai đoạn 3 — Hoàn thiện feature và recommendation

Thời gian: **14/10–27/10**.

### Người 1 — Leader

- Hoàn thiện Outbox, retry và dead-letter queue.
- Thêm logging theo correlation ID xuyên service.
- Tạo Docker Compose toàn hệ thống.
- Tạo seed data dùng chung.
- Kiểm tra security và service/database isolation.
- Điều phối end-to-end test.

### Người 2 — Identity và Catalog

- Admin API cho movie, showtime, room/seat, pricing và food.
- Pagination, filter và sort.
- Validation và authorization.
- Event schema/version.
- Swagger/OpenAPI và integration test.

### Người 3 — Booking và Payment

- Cancel/expire/refund.
- `REFUND_REQUESTED → REFUNDED | REFUND_FAILED`.
- Không double booking/double payment/double refund.
- Check-in một lần và chuyển Booking sang `USED`.
- Hoàn thiện Outbox cho event giao dịch.
- Concurrency và integration test.

### Người 4 — Web

- Profile, wishlist, review và recommendation section.
- Admin web MVP: movie, showtime, booking và food.
- Responsive và accessibility cơ bản.
- Hoàn thiện UI theo Stitch.
- Kiểm tra customer flow với dữ liệu thật.

### Người 5 — Flutter

- Profile.
- Wishlist và review.
- Recommendation section.
- Cache ảnh.
- Retry/offline message.
- Adaptive layout.
- Android development build đầu tiên.

### Người 6 — Recommendation và paper

- Tích hợp interaction events thật.
- Chạy popularity, content-based, collaborative, fixed hybrid và adaptive hybrid.
- Thử MMR re-ranking.
- Tính Precision@K, Recall@K, NDCG@K, MAP@K, Coverage và Diversity.
- Ghi model/data/config version.
- Viết Methodology, Experiment Setup và Results bản đầu.

Ngày 27/10 feature freeze. Sau mốc này không thêm feature lớn; chỉ sửa lỗi, hoàn thiện paper và cải thiện trải nghiệm.

## 7. Giai đoạn 4 — Ổn định và bàn giao

Thời gian: **28/10–11/11**.

### Người 1 — Leader

- Quản lý bug list theo mức Blocker/Critical/Major/Minor.
- Kiểm tra cài đặt từ môi trường sạch.
- Hoàn thiện deployment và architecture document.
- Khóa API/schema ngày 08/11.
- Chuẩn bị dữ liệu và môi trường demo.

### Người 2 — Identity và Catalog

- Fix lỗi Identity/Catalog.
- Kiểm tra migration từ database trống.
- Hoàn thiện seed data và OpenAPI.
- Đóng băng schema.

### Người 3 — Booking và Payment

- Load/concurrency test seat hold.
- Test callback lặp, callback trễ và retry payment.
- Test cancel/refund/check-in.
- Fix lỗi giao dịch.
- Chuẩn bị booking/payment demo data.

### Người 4 — Web

- Cross-browser test.
- User acceptance test.
- Fix responsive/UI consistency.
- Production build.

### Người 5 — Flutter

- Test trên nhiều kích thước màn hình.
- Test mất mạng, token hết hạn và app resume.
- Fix crash.
- Xuất APK/AAB và viết hướng dẫn cài đặt.

### Người 6 — Recommendation và paper

- Chạy lại thí nghiệm cuối.
- Hoàn thiện bảng/biểu đồ.
- Viết Discussion, Limitation và Conclusion.
- Kiểm tra khả năng tái tạo kết quả.
- Hoàn thiện paper trước 08/11.

## 8. Dependency quan trọng

```text
Leader khóa contract
    ├── Identity/Catalog API ──▶ Web/Flutter
    ├── Checkout Quote ────────▶ Booking
    └── Event Contract ────────▶ Recommendation

Catalog Quote ──▶ Booking ──▶ Payment ──▶ BookingPaid Event
                                          ├──▶ Recommendation
                                          ├──▶ Web
                                          └──▶ Flutter
```

Để không phải ngồi chờ:

- Web/Flutter dùng mock response đúng OpenAPI contract.
- Booking dùng mock checkout quote trước khi Catalog hoàn thành.
- Recommendation dùng mock event trước khi RabbitMQ hoạt động.
- Mọi thay đổi contract phải báo các consumer và được leader duyệt.

## 9. Definition of Done

Một task chỉ được đánh dấu Done khi:

- Có đầu ra chạy/xem được.
- Code đã push và có pull request.
- Có ít nhất một reviewer.
- Có validation và error handling phù hợp.
- Có test cho nghiệp vụ quan trọng.
- API/event đã cập nhật contract.
- Không truy cập database service khác.
- Frontend không tự quyết định giá hoặc tổng tiền.
- Chạy được trên môi trường local chung.

## 10. Cách leader theo dõi

Daily update ngắn:

```text
Hôm qua đã xong:
Hôm nay sẽ làm:
Đang bị chặn bởi:
Link PR/Stitch/tài liệu:
```

Mỗi tuần có hai lần integration check, không đợi cuối sprint mới ghép. Task dự kiến kéo dài quá hai ngày phải được tách nhỏ; task trễ hơn một ngày phải báo ngay để leader đổi người hỗ trợ hoặc giảm scope.

