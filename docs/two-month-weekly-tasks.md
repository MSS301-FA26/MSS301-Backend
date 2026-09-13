# CinemaAI — Task chi tiết từng tuần cho nhóm 6 người

Thời gian: **16/09–11/11**. Kế hoạch bắt đầu sau khi giảng viên kiểm tra Conceptual và Stitch ngày 15/09. Nếu giảng viên yêu cầu đổi service boundary, leader cập nhật kế hoạch và khóa lại scope trong ngày 16/09.

Xem cách tách monolith, giao tiếp giữa các service, chuyển dữ liệu và triển khai môi trường tại [microservice-implementation-plan.md](microservice-implementation-plan.md).

## 1. Mục tiêu cuối kỳ

Web và Flutter phải chạy được luồng xuyên suốt bằng hệ thống microservices:

~~~text
Đăng nhập → Xem phim → Chọn suất chiếu → Giữ ghế 3 phút
→ Chọn vé/đồ ăn → Thanh toán → Nhận QR → Xem lịch sử
→ Phát interaction/event → Nhận recommendation
~~~

Kiến trúc mục tiêu:

- API Gateway/BFF.
- Identity Service.
- Catalog Service.
- Booking Service.
- Payment Service.
- Recommendation Service.
- Mỗi service sở hữu PostgreSQL database/schema riêng.
- REST cho xử lý cần kết quả ngay; RabbitMQ cho domain event.
- Web hoàn thiện theo Stitch.
- Mobile Flutter hoàn thiện core customer flow.
- Paper recommendation là bonus, không được làm trễ sản phẩm chính.

## 2. Phân công cố định

| Người | Phần sở hữu chính | Phần hỗ trợ |
|---|---|---|
| Người 1 — Leader | Architecture, Gateway, integration, Docker, event platform | Payment/Outbox và xử lý blocker |
| Người 2 | Identity và Catalog backend | Checkout quote và Catalog event |
| Người 3 | Booking và Payment backend | Seat concurrency, snapshot và refund |
| Người 4 | Stitch Web và Web frontend | Kiểm tra design system cho Flutter |
| Người 5 | Stitch Mobile và Flutter | Kiểm tra UI Web và test API customer flow |
| Người 6 | Recommendation và paper | Interaction/event contract và recommendation API |

Owner chịu trách nhiệm phần của mình chạy được, có test/tài liệu và tích hợp được; không chỉ chịu trách nhiệm viết code.

## 3. Milestone chung

| Mốc | Kết quả bắt buộc |
|---|---|
| 16/09 | Cập nhật thay đổi theo nhận xét của giảng viên và khóa scope |
| 22/09 | API/event contract và môi trường local chung |
| 29/09 | Foundation hoàn thành; Web/Flutter dùng API thật hoặc mock đúng contract |
| 06/10 | Catalog, seat hold và booking draft chạy được |
| 13/10 | Demo end-to-end từ đăng nhập đến QR ticket |
| 20/10 | Recommendation tích hợp; feature admin/customer chính hoàn thành |
| 27/10 | Feature freeze; paper có kết quả thực nghiệm đầu tiên |
| 03/11 | Release candidate |
| 08/11 | Code/schema/design/paper freeze |
| 11/11 | Hoàn thành đồ án |

## 4. Quy ước làm việc

- Task code chỉ Done khi có PR, reviewer, test phù hợp và chạy được trên môi trường chung.
- Task UI chỉ Done khi có trạng thái bình thường, loading, empty và error.
- Web/Flutter dùng mock response đúng OpenAPI khi backend thật chưa xong.
- Booking dùng mock checkout quote trước khi Catalog hoàn thành.
- Recommendation dùng mock event trước khi RabbitMQ hoạt động.
- Mọi thay đổi contract phải báo các consumer và được leader duyệt.
- Task kéo dài quá hai ngày phải được chia nhỏ trên bảng công việc.

---

## Tuần 1 — 16/09 đến 22/09: khóa scope và dựng nền móng

### Người 1 — Leader

- **16/09:** Tổng hợp nhận xét của giảng viên, chốt phần làm và phần không làm.
- **17/09:** Chốt service boundary, entity ownership và database ownership.
- **18/09:** Chốt format API response/error, JWT, correlation ID và quy tắc version.
- **19/09:** Chốt event envelope, tên exchange, queue và routing key.
- **20–21/09:** Tạo cấu trúc source microservices, Docker Compose, PostgreSQL và RabbitMQ.
- **22/09:** Dựng Gateway route mẫu, health check và hướng dẫn chạy local.

### Người 2 — Identity và Catalog

- **16–17/09:** Liệt kê controller, service, repository và entity thuộc Identity/Catalog.
- **18/09:** Thiết kế Identity DB và Catalog DB, bỏ foreign key xuyên service.
- **19/09:** Viết OpenAPI register, login, refresh token và profile.
- **20/09:** Viết OpenAPI movie, search/filter và showtime.
- **21/09:** Viết OpenAPI room/seat, pricing và food.
- **22/09:** Review contract với Người 3, 4, 5 và sửa điểm không khớp.

### Người 3 — Booking và Payment

- **16–17/09:** Liệt kê code thuộc Booking, FoodOrder, Payment và Refund.
- **18/09:** Thiết kế Booking DB và Payment DB; xác định logical ID và snapshot.
- **19/09:** Chốt state machine Booking, Payment và Seat runtime.
- **20/09:** Viết OpenAPI hold, release và booking draft.
- **21/09:** Viết contract checkout, payment và booking history.
- **22/09:** Chốt contract checkout quote với Người 2.

### Người 4 — Web

- **16/09:** Kiểm kê màn hình Stitch Web đã có và danh sách còn thiếu.
- **17–18/09:** Chốt màu, font, spacing, button, input, card, modal và responsive breakpoint.
- **19/09:** Chia component dùng chung và cấu trúc route.
- **20/09:** Tạo API client, token handling và error model.
- **21–22/09:** Tạo mock data đúng OpenAPI cho login, movie và showtime.

### Người 5 — Flutter

- **16/09:** Chốt danh sách màn hình core mobile.
- **17/09:** Hoàn thiện Stitch Mobile cho login, home và movie detail.
- **18/09:** Hoàn thiện Stitch Mobile cho showtime, seat, checkout, payment result và QR.
- **19/09:** Khởi tạo Flutter với Riverpod, GoRouter, Dio và secure storage.
- **20/09:** Chuyển design token sang Flutter Theme.
- **21–22/09:** Tạo API client, error model và mock navigation core flow.

### Người 6 — Recommendation và paper

- **16/09:** Kiểm kê code và dữ liệu recommendation hiện tại.
- **17/09:** Chốt interaction schema: view, wishlist, booking, checked-in và review.
- **18/09:** Chốt movie features và cách xử lý dữ liệu thiếu.
- **19/09:** Chọn dataset chính/dataset thay thế và time-based split.
- **20/09:** Tạo pipeline làm sạch dữ liệu.
- **21–22/09:** Viết outline paper và mô tả bài toán nghiên cứu.

### Mốc kiểm tra tuần 1

- **20/09:** Review chéo contract lần một.
- **22/09:** Cả nhóm chạy được Gateway, PostgreSQL và RabbitMQ; Web/Flutter có mock đúng contract.

---

## Tuần 2 — 23/09 đến 29/09: Identity, Catalog và booking draft

### Người 1 — Leader

- **23/09:** Hoàn thiện Gateway route cho Identity và Catalog.
- **24/09:** Cấu hình JWT forwarding và CORS.
- **25/09:** Thiết lập log có correlation ID.
- **26/09:** Tạo integration environment và seed convention.
- **27–28/09:** Review database isolation, PR và contract implementation.
- **29/09:** Tổ chức demo đăng nhập, xem phim và giữ ghế.

### Người 2 — Identity và Catalog

- **23/09:** Tạo migration Identity và Catalog.
- **24/09:** Hoàn thành register, login và password hashing.
- **25/09:** Hoàn thành refresh token và profile.
- **26/09:** Hoàn thành movie list/detail và search/filter.
- **27/09:** Hoàn thành showtime, room và seat read API.
- **28/09:** Hoàn thành food và pricing read API.
- **29/09:** Unit test, seed data và sửa lỗi tích hợp.

### Người 3 — Booking và Payment

- **23/09:** Tạo migration Booking và Payment.
- **24/09:** Cài đặt seat availability theo showtime.
- **25/09:** Cài đặt hold và release ghế.
- **26/09:** Thêm holdExpiresAt bằng thời điểm tạo cộng 3 phút.
- **27/09:** Thêm job expire booking và giải phóng ghế.
- **28/09:** Hoàn thành booking draft và state transition ban đầu.
- **29/09:** Test hai user không giữ được cùng một ghế; cung cấp payment mock contract.

### Người 4 — Web

- **23/09:** Login/register bằng API thật.
- **24/09:** Home và movie card/component.
- **25/09:** Movie list, search và filter.
- **26/09:** Movie detail.
- **27/09:** Chọn ngày và suất chiếu.
- **28/09:** Seat map và các trạng thái ghế.
- **29/09:** Tích hợp API thật, sửa loading/empty/error.

### Người 5 — Flutter

- **23/09:** Login/register và lưu token.
- **24/09:** Home.
- **25/09:** Movie list và search.
- **26/09:** Movie detail.
- **27/09:** Chọn ngày và suất chiếu.
- **28/09:** Seat map component.
- **29/09:** Chạy core navigation bằng API thật hoặc mock đúng contract.

### Người 6 — Recommendation và paper

- **23/09:** Tạo popularity baseline.
- **24–25/09:** Tạo content-based bằng metadata/embedding.
- **26/09:** Tạo recommendation endpoint thử nghiệm.
- **27/09:** Tạo evaluation pipeline.
- **28/09:** Chạy time-based train/test split.
- **29/09:** Lưu kết quả baseline đầu tiên và cách tái chạy.

### Mốc kiểm tra tuần 2

- **26/09:** Web/Flutter gọi được Identity/Catalog trên integration environment.
- **29/09:** Demo qua Gateway: login → xem phim → chọn suất → giữ ghế.

---

## Tuần 3 — 30/09 đến 06/10: checkout và snapshot

### Người 1 — Leader

- **30/09:** Khóa version contract checkout.
- **01/10:** Dựng RabbitMQ exchange/queue convention.
- **02/10:** Tạo publisher/consumer mẫu.
- **03/10:** Tạo mẫu Transactional Outbox và idempotent consumer.
- **04–05/10:** Tích hợp Catalog–Booking và theo dõi lỗi timeout.
- **06/10:** Tổ chức checkpoint checkout hoàn chỉnh.

### Người 2 — Identity và Catalog

- **30/09:** Thiết kế request batch checkout quote.
- **01/10:** Validate showtime, room, seat và movie đang active.
- **02/10:** Validate ticket pricing, food/combo và quantity.
- **03/10:** Trả quoteId, priceVersion và validUntil.
- **04/10:** Trả đầy đủ dữ liệu snapshot và tổng tiền thành phần.
- **05/10:** Integration test quote hợp lệ, hết hạn và dữ liệu không active.
- **06/10:** Sửa contract sau khi tích hợp Booking.

### Người 3 — Booking và Payment

- **30/09:** Tạo client gọi Catalog quote có timeout.
- **01/10:** Kiểm tra quote còn hiệu lực.
- **02/10:** Backend tự tính subtotal, discount và total.
- **03/10:** Lưu snapshot movie, showtime, room, seat, ticket và food.
- **04/10:** Chuyển HOLDING sang PENDING_PAYMENT đúng state machine.
- **05/10:** Test quote sai, giá đổi, hold hết hạn và Catalog lỗi.
- **06/10:** Demo booking checkout bằng API thật.

### Người 4 — Web

- **30/09:** Gọi API hold và hiển thị countdown 3 phút.
- **01/10:** Xử lý ghế bị người khác giữ và hold hết hạn.
- **02/10:** UI chọn ticket type.
- **03/10:** UI chọn food/combo.
- **04/10:** Trang checkout lấy toàn bộ giá từ backend.
- **05–06/10:** Test và sửa luồng từ seat đến PENDING_PAYMENT.

### Người 5 — Flutter

- **30/09:** Gọi hold API và countdown 3 phút.
- **01/10:** Xử lý conflict, expire và app resume.
- **02/10:** Màn hình chọn ticket type.
- **03/10:** Màn hình chọn food/combo.
- **04/10:** Checkout lấy giá từ backend.
- **05–06/10:** Test luồng trên emulator/điện thoại.

### Người 6 — Recommendation và paper

- **30/09:** Chuẩn hóa implicit feedback weights.
- **01–02/10:** Cài collaborative filtering bằng BPR hoặc LightFM.
- **03/10:** Xử lý user/item cold-start.
- **04/10:** Tạo fixed hybrid baseline.
- **05/10:** Tạo mock consumer cho Catalog và Booking events.
- **06/10:** So sánh kết quả CB, CF và fixed hybrid ban đầu.

### Mốc kiểm tra tuần 3

- **03/10:** Contract checkout không còn thay đổi lớn.
- **06/10:** Booking lưu được snapshot, tổng tiền đúng và không double hold.

---

## Tuần 4 — 07/10 đến 13/10: thanh toán và QR end-to-end

### Người 1 — Leader

- **07/10:** Chốt contract Payment–Booking và callback.
- **08/10:** Kiểm tra secret/config không nằm trong source.
- **09/10:** Tích hợp RabbitMQ PaymentSucceeded.
- **10/10:** Thêm log xuyên Gateway–Payment–Booking.
- **11–12/10:** Chạy integration test và xử lý blocker.
- **13/10:** Điều phối demo Web và Flutter từ login đến QR.

### Người 2 — Identity và Catalog

- **07/10:** Phát MoviePublished và MovieUpdated.
- **08/10:** Phát ShowtimeCancelled và FoodPriceChanged.
- **09/10:** Thêm event version và correlation ID.
- **10/10:** Hoàn thiện test checkout quote.
- **11–12/10:** Hỗ trợ sửa lỗi dữ liệu Catalog trong luồng booking.
- **13/10:** Chuẩn bị seed movie/showtime/seat/food cho demo.

### Người 3 — Booking và Payment

- **07/10:** Tạo payment session MOCK/VNPay development.
- **08/10:** Xác minh callback signature và amount.
- **09/10:** Bảo đảm callback lặp không ghi nhận hai lần.
- **10/10:** Phát PaymentSucceeded/PaymentFailed bằng Outbox.
- **11/10:** Booking nhận event, chuyển PAID và ghế BOOKED.
- **12/10:** Sinh QR, booking history và booking detail.
- **13/10:** Test success, failed, cancelled, callback lặp và callback trễ.

### Người 4 — Web

- **07/10:** Gọi API tạo payment và redirect.
- **08/10:** Payment success/failed/cancelled screen.
- **09/10:** Poll hoặc refresh trạng thái booking sau callback.
- **10/10:** Booking history.
- **11/10:** Booking detail và QR.
- **12–13/10:** End-to-end test và sửa UI.

### Người 5 — Flutter

- **07/10:** Tích hợp payment URL/deep-link phù hợp.
- **08/10:** Payment result screen.
- **09/10:** Refresh trạng thái booking khi quay lại app.
- **10/10:** Booking history.
- **11/10:** Booking detail và QR.
- **12–13/10:** Test token expire, mất mạng và app resume.

### Người 6 — Recommendation và paper

- **07/10:** Consumer MoviePublished/MovieUpdated.
- **08/10:** Consumer BookingPaid.
- **09/10:** Lưu interaction idempotent.
- **10/10:** Tạo adaptive alpha theo số interaction.
- **11/10:** Thêm API recommendation có fallback popularity.
- **12–13/10:** Tích hợp recommendation test với event thật.

### Mốc kiểm tra tuần 4

- **10/10:** Backend hoàn thành payment success trên integration environment.
- **13/10:** Web và Flutter đều demo được login → booking → payment → QR bằng API thật.

---

## Tuần 5 — 14/10 đến 20/10: feature chính và recommendation

### Người 1 — Leader

- **14/10:** Chốt scope feature còn lại, loại bỏ phần không kịp.
- **15/10:** Review authorization cho customer, staff và admin.
- **16/10:** Hoàn thiện Docker Compose toàn hệ thống.
- **17/10:** Tạo seed data chung có ID ổn định.
- **18–19/10:** Chạy integration test cancel/refund/check-in.
- **20/10:** Review tiến độ feature và paper.

### Người 2 — Identity và Catalog

- **14/10:** Admin CRUD movie/genre/actor.
- **15/10:** Admin CRUD room/seat.
- **16/10:** Admin CRUD showtime.
- **17/10:** Admin CRUD pricing và food/combo.
- **18/10:** Pagination, filter và sort.
- **19–20/10:** Validation, authorization, Swagger và test.

### Người 3 — Booking và Payment

- **14/10:** Cancel và expire flow.
- **15/10:** Refund request và refund state machine.
- **16/10:** Xử lý refund success/failure idempotent.
- **17/10:** Check-in một lần cho booking PAID.
- **18/10:** Hoàn thiện FoodOrder và pickup flow trong scope.
- **19–20/10:** Test không double payment/refund/check-in.

### Người 4 — Web

- **14/10:** Profile.
- **15/10:** Wishlist.
- **16/10:** Review.
- **17/10:** Recommendation section.
- **18/10:** Admin movie/showtime.
- **19/10:** Admin booking/food.
- **20/10:** Responsive và test dữ liệu thật.

### Người 5 — Flutter

- **14/10:** Profile.
- **15/10:** Wishlist.
- **16/10:** Review.
- **17/10:** Recommendation section.
- **18/10:** Cache ảnh và retry.
- **19/10:** Offline/error message và adaptive layout.
- **20/10:** Xuất Android development build đầu tiên.

### Người 6 — Recommendation và paper

- **14/10:** Nhận interaction thật từ Web/Flutter/Booking.
- **15/10:** Chạy popularity và content-based trên cùng split.
- **16/10:** Chạy collaborative và fixed hybrid.
- **17/10:** Chạy adaptive hybrid.
- **18/10:** Thử MMR re-ranking.
- **19/10:** Tính Precision@K, Recall@K, NDCG@K, MAP@K, Coverage và Diversity.
- **20/10:** Viết Methodology và Experiment Setup bản đầu.

### Mốc kiểm tra tuần 5

- **17/10:** Core admin API và cancel/refund/check-in chạy được.
- **20/10:** Recommendation xuất kết quả trên dữ liệu đã chốt; Web/Flutter hiển thị được.

---

## Tuần 6 — 21/10 đến 27/10: hoàn thiện độ tin cậy và feature freeze

### Người 1 — Leader

- **21/10:** Kiểm tra tất cả service chỉ dùng database của mình.
- **22/10:** Hoàn thiện retry có giới hạn và dead-letter queue.
- **23/10:** Kiểm tra correlation ID xuyên service.
- **24/10:** Review bảo mật, CORS, secret và internal endpoint.
- **25/10:** Lập danh sách bug theo Blocker/Critical/Major/Minor.
- **26/10:** Điều phối full end-to-end regression.
- **27/10:** Khóa feature; chỉ cho phép sửa lỗi sau mốc này.

### Người 2 — Identity và Catalog

- **21/10:** Hoàn thiện migration PostgreSQL/Flyway.
- **22/10:** Chuyển Hibernate sang validate trong môi trường release.
- **23/10:** Test token, role và dữ liệu inactive.
- **24/10:** Test Catalog event schema/version.
- **25–26/10:** Fix lỗi integration và hoàn thiện Swagger.
- **27/10:** Chốt schema/API Identity–Catalog.

### Người 3 — Booking và Payment

- **21/10:** Hoàn thiện Transactional Outbox.
- **22/10:** Hoàn thiện idempotent consumer.
- **23/10:** Test race condition giữ ghế.
- **24/10:** Test callback lặp/trễ và payment retry.
- **25/10:** Test cancel/refund/check-in.
- **26/10:** Fix lỗi integration.
- **27/10:** Chốt schema/API Booking–Payment.

### Người 4 — Web

- **21/10:** Hoàn thiện tất cả trạng thái loading/empty/error.
- **22/10:** Accessibility: label, keyboard, contrast và focus.
- **23/10:** Responsive mobile/tablet/desktop.
- **24/10:** Kiểm tra UI so với Stitch.
- **25–26/10:** Regression toàn customer/admin flow.
- **27/10:** Chốt feature Web.

### Người 5 — Flutter

- **21/10:** Test app resume trong lúc giữ ghế/thanh toán.
- **22/10:** Test token hết hạn.
- **23/10:** Test mất mạng và retry.
- **24/10:** Test nhiều kích thước màn hình.
- **25–26/10:** Regression customer flow trên thiết bị/emulator.
- **27/10:** Chốt feature Mobile.

### Người 6 — Recommendation và paper

- **21/10:** Chạy lại toàn bộ model với config cố định.
- **22/10:** Kiểm tra cold-start và popularity fallback.
- **23/10:** Ghi data/model/config version.
- **24/10:** Tạo bảng và biểu đồ kết quả.
- **25/10:** Viết Results.
- **26/10:** Viết Discussion và Limitation bản đầu.
- **27/10:** Chốt thí nghiệm chính.

### Mốc kiểm tra tuần 6

- **24/10:** Security và reliability review.
- **27/10:** Feature freeze; có bảng kết quả paper đầu tiên.

---

## Tuần 7 — 28/10 đến 03/11: release candidate

### Người 1 — Leader

- **28/10:** Tạo release checklist và phân loại toàn bộ bug.
- **29/10:** Test cài đặt từ máy/database sạch.
- **30/10:** Chạy full integration test.
- **31/10:** Kiểm tra log, health check và failure recovery.
- **01–02/11:** Điều phối sửa Blocker/Critical.
- **03/11:** Đóng gói release candidate.

### Người 2 — Identity và Catalog

- **28/10:** Test migration từ database trống.
- **29/10:** Test seed data chạy lặp an toàn.
- **30/10:** Test Auth/Catalog regression.
- **31/10:** Kiểm tra API permission và validation.
- **01–02/11:** Sửa bug ưu tiên.
- **03/11:** Bàn giao OpenAPI và migration bản RC.

### Người 3 — Booking và Payment

- **28/10:** Load/concurrency test seat hold.
- **29/10:** Test booking expire và giải phóng ghế.
- **30/10:** Test callback lặp/trễ/sai chữ ký/sai amount.
- **31/10:** Test refund và check-in.
- **01–02/11:** Sửa bug giao dịch ưu tiên.
- **03/11:** Bàn giao booking/payment demo data.

### Người 4 — Web

- **28/10:** Cross-browser test.
- **29/10:** User acceptance test customer flow.
- **30/10:** User acceptance test admin flow.
- **31/10:** Fix responsive và UI consistency.
- **01–02/11:** Sửa crash và lỗi luồng.
- **03/11:** Tạo Web production build RC.

### Người 5 — Flutter

- **28/10:** Test nhiều phiên bản/kích thước Android.
- **29/10:** Test cài mới, logout/login và token expire.
- **30/10:** Test mất mạng, app resume và payment return.
- **31/10:** Fix crash và layout.
- **01–02/11:** Sửa lỗi luồng ưu tiên.
- **03/11:** Xuất APK RC.

### Người 6 — Recommendation và paper

- **28/10:** Chạy lại thí nghiệm từ môi trường sạch.
- **29/10:** Kiểm tra kết quả có thể tái tạo.
- **30/10:** Hoàn thiện bảng/biểu đồ.
- **31/10:** Viết Discussion và Limitation.
- **01/11:** Viết Conclusion.
- **02–03/11:** Review paper và đối chiếu số liệu.

### Mốc kiểm tra tuần 7

- **31/10:** Không còn Blocker trong core flow.
- **03/11:** Có release candidate Web, APK và backend Compose.

---

## Tuần 8 — 04/11 đến 11/11: ổn định và bàn giao

### Người 1 — Leader

- **04/11:** Chạy thử toàn bộ hệ thống từ repository sạch.
- **05/11:** Hoàn thiện architecture/deployment document.
- **06/11:** Chuẩn bị tài khoản, seed data và kịch bản demo.
- **07/11:** Điều phối buổi rehearsal toàn nhóm.
- **08/11:** Khóa code, schema và design.
- **09–10/11:** Chỉ nhận sửa lỗi Critical có kiểm soát.
- **11/11:** Kiểm tra và bàn giao bản cuối.

### Người 2 — Identity và Catalog

- **04/11:** Fix bug còn lại.
- **05/11:** Chạy lại migration và seed data.
- **06/11:** Kiểm tra Swagger/API examples.
- **07/11:** Hỗ trợ rehearsal.
- **08/11:** Đóng băng schema.
- **09–11/11:** Hỗ trợ lỗi release nếu có.

### Người 3 — Booking và Payment

- **04/11:** Fix bug giao dịch còn lại.
- **05/11:** Chạy lại concurrency/payment/refund test.
- **06/11:** Chuẩn bị tình huống demo success/failure.
- **07/11:** Hỗ trợ rehearsal.
- **08/11:** Đóng băng schema.
- **09–11/11:** Chỉ sửa lỗi Critical.

### Người 4 — Web

- **04/11:** Fix UI còn lại.
- **05/11:** Kiểm tra production environment.
- **06/11:** Chuẩn bị dữ liệu/màn hình demo.
- **07/11:** Rehearsal Web.
- **08/11:** Tạo production build cuối.
- **09–11/11:** Chỉ sửa lỗi Critical.

### Người 5 — Flutter

- **04/11:** Fix crash/layout còn lại.
- **05/11:** Kiểm tra API base URL và release config.
- **06/11:** Viết hướng dẫn cài đặt.
- **07/11:** Rehearsal Mobile.
- **08/11:** Xuất APK/AAB cuối.
- **09–11/11:** Chỉ sửa lỗi Critical.

### Người 6 — Recommendation và paper

- **04/11:** Kiểm tra lại công thức, số liệu và nguồn dataset.
- **05/11:** Hoàn thiện abstract/introduction/related work theo phạm vi.
- **06/11:** Hoàn thiện phương pháp, kết quả và thảo luận.
- **07/11:** Kiểm tra citation và khả năng tái tạo.
- **08/11:** Chốt paper.
- **09–11/11:** Hỗ trợ demo recommendation và sửa lỗi trình bày cuối.

### Mốc kiểm tra tuần 8

- **07/11:** Rehearsal thành công bằng môi trường và dữ liệu thật.
- **08/11:** Code/schema/design/paper freeze.
- **11/11:** Bàn giao project hoàn chỉnh.

---

## Quan hệ phụ thuộc cần leader theo dõi

| Bên giao | Đầu ra | Người đang chờ |
|---|---|---|
| Người 1 | API/event convention, Gateway, môi trường | Toàn nhóm |
| Người 2 | Auth/Catalog API và checkout quote | Người 3, 4, 5, 6 |
| Người 3 | Hold/booking/payment API và event | Người 4, 5, 6 |
| Người 4 | Web interaction đúng nghiệp vụ | Người 3, 6 |
| Người 5 | Mobile interaction đúng nghiệp vụ | Người 3, 6 |
| Người 6 | Recommendation API | Người 4, 5 |

Khi đầu ra thật chưa xong, bên đang chờ phải dùng mock đúng contract. Không tự sửa tên field hoặc trạng thái mà chưa báo owner và leader.

## Cách leader kiểm tra mỗi ngày

Mỗi thành viên gửi bốn dòng:

    Hôm qua đã xong:
    Hôm nay sẽ làm:
    Đang bị chặn bởi:
    Link PR/Stitch/tài liệu:

Cuối mỗi tuần, leader chỉ chốt Done khi có demo ngắn trên integration environment. Phần chưa chạy được phải chuyển thành bug/task có owner và deadline mới; không ghi chung là gần xong.

## Definition of Done

Một task chỉ được đánh dấu Done khi:

- Có đầu ra chạy hoặc xem được.
- Code đã push và có pull request.
- Có ít nhất một reviewer.
- Có validation và error handling phù hợp.
- Có test cho nghiệp vụ quan trọng.
- API/event contract đã được cập nhật.
- Không truy cập database của service khác.
- Frontend không tự quyết định giá hoặc tổng tiền.
- Chạy được trên môi trường local/integration chung.

Mỗi tuần có hai lần integration check. Task trễ hơn một ngày phải báo ngay để leader đổi người hỗ trợ hoặc giảm scope.
