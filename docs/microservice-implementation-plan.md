# CinemaAI — Cách triển khai project theo Microservices trong 2 tháng

Tài liệu này trả lời câu hỏi: từ project Spring Boot monolith hiện tại, nhóm sẽ tách và triển khai thành microservices như thế nào trong thời gian **16/09–11/11**.

## 1. Cách tiếp cận

Nhóm áp dụng cách **tách dần (Strangler Pattern)**:

1. Giữ monolith hiện tại làm nguồn đối chiếu nghiệp vụ và API.
2. Chốt contract trước khi tách code.
3. Tách từng vùng nghiệp vụ thành service độc lập.
4. Web và Flutter chỉ gọi qua API Gateway.
5. Khi một luồng trên service mới chạy ổn định, không dùng phần tương ứng trong monolith nữa.

Không sao chép nguyên project monolith thành năm project rồi xóa code thừa. Mỗi service chỉ mang theo entity, repository, business rule và API thuộc phạm vi của mình.

Mục tiêu cuối cùng là core flow dưới đây chạy hoàn toàn bằng hệ thống microservices:

```text
Đăng nhập → Xem phim → Chọn suất → Giữ ghế 3 phút
→ Chọn vé/đồ ăn → Thanh toán → Nhận QR → Xem lịch sử
→ Gửi interaction → Nhận danh sách phim gợi ý
```

## 2. Cấu trúc hệ thống mục tiêu

```text
Web / Flutter
      │
      ▼
API Gateway
      ├── Identity Service ─────── Identity DB
      ├── Catalog Service ──────── Catalog DB
      ├── Booking Service ──────── Booking DB
      ├── Payment Service ──────── Payment DB
      └── Recommendation Service ─ Recommendation DB

Catalog / Booking / Payment ── event ── RabbitMQ ── Recommendation
Payment ── HTTPS ── VNPay
```

Trong phạm vi đồ án hai tháng, nhóm dùng **Docker Compose**, chưa cần Kubernetes. Mỗi service là một container, có cấu hình và database riêng. Chỉ Gateway được Web/Flutter truy cập trực tiếp; PostgreSQL và RabbitMQ chỉ nằm trong mạng nội bộ của hệ thống.

## 3. Phạm vi của từng service

### Identity Service

Sở hữu tài khoản và xác thực:

- User, Profile, Role, UserRole.
- Staff identity.
- Register, login, refresh token, đổi/quên mật khẩu.
- Phát JWT; Gateway và các service xác minh chữ ký JWT.

Identity không truy vấn booking hoặc payment. Các service khác chỉ lưu `userId` dạng logical ID.

### Catalog Service

Sở hữu dữ liệu dùng để bán:

- Movie, Genre, Actor.
- Cinema, Room, Seat.
- Showtime.
- TicketPricingRule, TicketCombo.
- FoodItem, FoodCombo.

Catalog cung cấp API đọc cho khách, API quản trị và một API nội bộ `checkout-quote` để Booking kiểm tra toàn bộ lựa chọn và lấy giá chính xác chỉ trong một lần gọi.

### Booking Service

Sở hữu quá trình đặt chỗ:

- Booking, BookingSeat, BookingTicket, BookingFoodItem.
- FoodOrder.
- Seat hold theo từng showtime.
- Trạng thái booking, lịch sử, QR và check-in.

Booking lưu `userId`, `movieId`, `showtimeId`, `seatId`, `productId` dưới dạng logical ID; không tạo foreign key sang database khác.

Các giá trị cần giữ đúng theo thời điểm giao dịch được lưu snapshot, ví dụ:

```text
productId
productNameSnapshot
unitPriceSnapshot
quantity
lineTotal
```

Tương tự, booking lưu snapshot tên phim, thời gian chiếu, phòng, nhãn ghế và giá vé. Vì vậy, khi Catalog đổi tên hoặc đổi giá, hóa đơn cũ vẫn đúng và trang lịch sử không phải gọi nhiều service để ghép dữ liệu.

### Payment Service

Sở hữu giao dịch tài chính:

- Payment, PaymentAttempt, Refund.
- Thông tin giao dịch VNPay.
- Wallet/Loyalty được giữ trong service này ở phiên bản đồ án để không phát sinh thêm một service.

Payment không cập nhật trực tiếp Booking DB. Sau khi xác minh chữ ký, số tiền và trạng thái callback VNPay, Payment lưu kết quả rồi phát `PaymentSucceeded` hoặc `PaymentFailed`.

### Recommendation Service

Tiếp tục phát triển từ `AiService` hiện tại:

- Interaction Store.
- Movie Feature Read Model.
- Popularity, content-based, collaborative filtering và hybrid.
- Recommendation result và model version.
- Wishlist/Review có thể nằm trong vùng Engagement của service này trong phạm vi đồ án vì đều là tín hiệu theo user–movie.

Recommendation không đọc trực tiếp database của Booking hoặc Catalog. Nó nhận metadata và hành vi qua event/API đã thống nhất.

### Những phần chưa cần tách riêng

- Notification và Audit giữ làm supporting module hoặc consumer nhỏ.
- Không tạo service riêng cho mỗi entity.
- Không tách Loyalty, Review hoặc Wishlist thành service độc lập trong hai tháng.

## 4. Service giao tiếp như thế nào

### REST đồng bộ

Dùng khi người dùng đang chờ kết quả ngay:

- Gateway gọi Identity/Catalog/Booking/Payment/Recommendation.
- Booking gọi một lần sang Catalog để lấy `checkout-quote`.
- Payment hỏi Booking xem đơn còn được thanh toán hay không.
- Recommendation API trả danh sách phim cho Web/Flutter.

Mỗi lời gọi phải có timeout rõ ràng. Không tạo chuỗi gọi dài kiểu A gọi B, B gọi C, C lại gọi D.

### RabbitMQ bất đồng bộ

Dùng cho việc không cần trả kết quả ngay trong request hiện tại:

- `MoviePublished`, `MovieUpdated` cập nhật feature cho Recommendation.
- `BookingPaid`, `TicketCheckedIn` tạo tín hiệu recommendation/loyalty.
- `PaymentSucceeded`, `PaymentFailed` cập nhật Booking.
- `BookingExpired`, `BookingCancelled` phục vụ notification/analytics.

Event có envelope chung:

```json
{
  "eventId": "uuid",
  "eventType": "BookingPaid",
  "version": 1,
  "occurredAt": "ISO-8601",
  "aggregateId": "booking-id",
  "correlationId": "request-id",
  "payload": {}
}
```

Producer dùng Transactional Outbox cho event giao dịch quan trọng. Consumer lưu `eventId` đã xử lý để một event gửi lặp không tạo double booking, double point hoặc double refund.

## 5. Luồng đặt vé thực tế

```text
1. Web/Flutter lấy phim, suất, ghế và món ăn từ Catalog qua Gateway.
2. Client gửi showtimeId, seatIds, ticket type, productId và quantity cho Booking.
3. Booking khóa dữ liệu giữ ghế và kiểm tra xung đột.
4. Booking gọi batch checkout-quote sang Catalog.
5. Catalog kiểm tra dữ liệu còn active và trả giá có thẩm quyền.
6. Booking tự tính tổng, lưu snapshot và tạo HOLDING trong 3 phút.
7. Client yêu cầu Payment tạo URL VNPay cho bookingId.
8. Payment kiểm tra booking còn payable, sau đó tạo giao dịch.
9. VNPay callback về Payment; Payment xác minh và lưu SUCCESS theo cách idempotent.
10. Payment phát PaymentSucceeded.
11. Booking nhận event, chuyển sang PAID, chốt ghế BOOKED và tạo QR.
12. Booking phát BookingPaid; Recommendation ghi nhận tín hiệu.
```

Client không gửi `productName`, `unitPrice` hoặc `totalAmount` làm nguồn dữ liệu tin cậy. Các giá trị này phải do backend lấy và tính.

## 6. Cách xử lý dữ liệu khi tách monolith

### Giai đoạn phát triển

- Tạo schema/database mới cho từng service.
- Viết migration PostgreSQL mới bằng Flyway.
- Tạo seed data chung với ID ổn định để các service tham chiếu logic.
- Không dùng `ddl-auto=update` làm nguồn schema chính; mục tiêu là Flyway + `ddl-auto=validate`.
- Không giữ foreign key xuyên database.

### Cách chuyển dữ liệu

1. Export dữ liệu gốc từ monolith.
2. Import Identity trước.
3. Import Catalog sau.
4. Import Booking với các logical ID và bổ sung snapshot cần thiết.
5. Import Payment theo `bookingId`.
6. Tạo interaction/read model cho Recommendation từ lịch sử booking, wishlist, review.
7. Đối soát số lượng bản ghi và tổng tiền trước khi chuyển sang service mới.

Vì đây là đồ án, nhóm nên viết script/migration có thể chạy lại trên database sạch; không cần xây hệ thống đồng bộ dữ liệu hai chiều phức tạp.

## 7. Cấu trúc source code đề xuất

Nhóm sáu người nên dùng một repository để dễ review và chạy integration:

```text
services/
  api-gateway/
  identity-service/
  catalog-service/
  booking-service/
  payment-service/
  recommendation-service/
clients/
  web/
  mobile/
contracts/
  openapi/
  events/
infra/
  docker-compose.yml
  rabbitmq/
docs/
```

Không tạo thư viện dùng chung chứa JPA entity. Nếu chia sẻ quá nhiều code domain, các service sẽ bị dính chặt như monolith cũ. Chỉ chia sẻ những thứ thật sự kỹ thuật và ổn định, hoặc đơn giản giữ contract bằng OpenAPI/JSON Schema.

## 8. Triển khai theo tuần

### Tuần 1 — 16/09 đến 22/09: khóa nền móng

- Sửa conceptual theo nhận xét của giảng viên và khóa scope.
- Chốt service ownership, API OpenAPI và event schema.
- Tạo repository structure, Docker Compose, PostgreSQL và RabbitMQ.
- Gateway route mẫu; mỗi service có `/actuator/health`.
- Web/Flutter dùng mock đúng contract.

Kết quả: toàn nhóm chạy được cùng một môi trường và không phải tự đoán request/response.

### Tuần 2 — 23/09 đến 29/09: tách Identity và Catalog

- Identity chạy register/login/refresh/profile.
- Catalog chạy movie/showtime/room/seat/food/pricing read API.
- Booking có hold ghế và booking draft.
- Web/Flutter hoàn thành luồng xem phim đến chọn ghế.
- Recommendation có popularity/content-based baseline.

Kết quả: đăng nhập, xem phim và giữ ghế được qua Gateway.

### Tuần 3 — 30/09 đến 06/10: hoàn thành checkout

- Catalog có batch checkout-quote.
- Booking lưu snapshot và kiểm tra giá ở backend.
- Hoàn thiện concurrency: hai người không giữ cùng một ghế.
- Web/Flutter có countdown 3 phút, chọn vé/đồ ăn và trang checkout.
- RabbitMQ có convention exchange/queue và event mẫu.

Kết quả: tạo được booking hợp lệ với tổng tiền đúng.

### Tuần 4 — 07/10 đến 13/10: thanh toán end-to-end

- Payment mock/VNPay development flow.
- Callback kiểm tra chữ ký, amount và idempotency.
- `PaymentSucceeded` cập nhật Booking thành `PAID`.
- Sinh QR, lịch sử booking, payment result trên Web và Flutter.

Kết quả bắt buộc: cả Web và Flutter demo được từ login đến QR bằng API thật.

### Tuần 5 — 14/10 đến 20/10: feature và recommendation

- Admin quản lý movie, showtime, room/seat, pricing và food.
- Booking có cancel/expire/refund/check-in.
- Recommendation nhận event thật và chạy collaborative + adaptive hybrid.
- Web/Flutter tích hợp wishlist, review và recommendation.

Kết quả: đủ feature chính; recommender có dữ liệu thực tế hoặc dataset thay thế được mô tả rõ.

### Tuần 6 — 21/10 đến 27/10: đóng feature

- Hoàn thiện Outbox, retry, dead-letter queue và correlation ID.
- Kiểm tra quyền truy cập, database isolation và lỗi mạng.
- Chạy thí nghiệm recommender và có bảng kết quả đầu tiên.
- Ngày 27/10 feature freeze.

Kết quả: không còn thêm feature lớn; chuyển sang sửa lỗi và hoàn thiện paper.

### Tuần 7 — 28/10 đến 03/11: release candidate

- Integration test toàn luồng.
- Load/concurrency test giữ ghế.
- Test callback lặp/trễ, payment retry, refund và check-in.
- Web cross-browser; Flutter test nhiều kích thước/mất mạng/app resume.
- Paper hoàn thiện Methodology, Experiment và Results.

Kết quả: có release candidate chạy được từ môi trường sạch.

### Tuần 8 — 04/11 đến 11/11: ổn định và bàn giao

- Chỉ sửa bug, không đổi contract tùy ý.
- Khóa code/schema/design ngày 08/11.
- Build Web production và Flutter APK/AAB.
- Chạy lại thí nghiệm cuối và hoàn thiện paper.
- Chuẩn bị seed data, tài khoản và kịch bản demo.

Kết quả: `docker compose up` dựng được backend hoàn chỉnh; Web và Flutter kết nối được; tài liệu đủ để người khác chạy lại.

## 9. Cách deploy phù hợp với đồ án

### Local và integration

Docker Compose chạy:

- 1 API Gateway.
- 4 Spring Boot service.
- 1 FastAPI Recommendation service.
- PostgreSQL theo từng service.
- RabbitMQ.
- Web frontend.

Flutter chạy trên emulator/điện thoại và trỏ về Gateway. Biến môi trường chứa URL và secret; repository chỉ lưu `.env.example`, không commit secret thật.

### Môi trường demo

Nếu nhóm có server/cloud, deploy chính cấu hình Compose lên một máy Linux. Nếu không, một laptop nhóm chạy Compose vẫn đủ cho đồ án. Kubernetes, service mesh, distributed tracing platform lớn và auto-scaling không phải ưu tiên trong hai tháng.

Điều bắt buộc phải có:

- Health check.
- Log có `correlationId`.
- Timeout/retry có giới hạn.
- Database riêng và migration chạy được.
- Không public database/RabbitMQ ra Internet.
- Backup/seed dữ liệu demo.

## 10. Thứ tự ưu tiên khi thiếu thời gian

1. Luồng login → booking → payment → QR chạy đúng.
2. Không double booking/double payment; giá do backend quyết định.
3. Web và Flutter cùng dùng được API thật.
4. Recommendation có baseline, hybrid và evaluation tái tạo được.
5. Admin/refund/check-in.
6. UI phụ và tối ưu nâng cao.

Paper là bonus nên chạy song song nhưng không được làm chậm core product. Nếu trễ, giảm tính năng UI phụ hoặc số thuật toán thử nghiệm; không bỏ kiểm tra giao dịch và tính nhất quán dữ liệu.

## 11. Điều kiện coi là hoàn thành

Project chỉ được coi là chuyển sang microservices khi:

- Core flow không còn phụ thuộc runtime monolith.
- Mỗi service khởi động, migrate database và health check độc lập.
- Mỗi service chỉ truy cập database của mình.
- Web/Flutter chỉ gọi Gateway.
- Booking lưu snapshot và không tin giá từ client.
- Payment callback và consumer event xử lý lặp an toàn.
- Có ít nhất một integration test cho luồng từ hold ghế đến payment thành công.
- Hệ thống dựng được từ môi trường sạch bằng tài liệu của nhóm.

