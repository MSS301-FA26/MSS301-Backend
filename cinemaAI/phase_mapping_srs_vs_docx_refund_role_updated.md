# Mapping phase DOCX theo SRS_CINEMA_SYSTEM_COMPLETE và ROLE_BASED_BUSINESS_FLOWS

Nguồn đối chiếu:
- DOCX ban đầu: `D:\FPTK8\SBA301\BE-LastUpdate.docx`
- SRS cập nhật: `SRS_CINEMA_SYSTEM_COMPLETE.md`
- Role flow cập nhật: `ROLE_BASED_BUSINESS_FLOWS.md`
- Prompt nghiệp vụ mở rộng: `Incident-based Refund / Refund Claim / Refund History / Notification`

Ngày cập nhật: 25/06/2026

---

## 0. Nguyên tắc mapping mới

DOCX chia theo phase triển khai backend, còn SRS và role flow chia theo module nghiệp vụ. Vì vậy file này dùng nguyên tắc:

1. Mỗi phase giữ vai trò chính theo roadmap backend.
2. Module nào liên quan nhiều phase sẽ có **Main Phase**, **Dependency Phase** và **Related Phase**.
3. Refund được xác định là module thuộc **PHASE 6 - PAYMENT & REFUND**, nhưng có liên quan trực tiếp tới booking, showtime, notification, loyalty, audit và QA.
4. Role trong code/SRS dùng `CUSTOMER`, không dùng `USER` làm tên role chính.
5. Hệ thống là mô hình **một rạp duy nhất**, không có chọn rạp/thành phố/chi nhánh.
6. Runtime database là **PostgreSQL**. Nếu làm migration phải tương thích PostgreSQL.

---

## PHASE 0 - SHARED FOUNDATION

### Tương ứng trong SRS

- Mục 1. Mục đích tài liệu.
- Mục 2. Tổng quan hệ thống.
- Mục 2.1. Quy tắc single-cinema.
- Mục 2.2. Công nghệ hiện tại.
- Mục 3. Actors và phân quyền.
- Mục 4. Kiến trúc và thành phần.
- Mục 5.1. Shared Foundation và chuẩn API.
- Mục 6. Yêu cầu phi chức năng.

### Trạng thái hiện tại

**ĐÃ HOÀN THÀNH** ở mức nền tảng.

### Ghi chú mapping

- API foundation, response/error format, Swagger, request logging và security nền đã có.
- Mọi phase sau phải tuân thủ:
  - REST API version `/api/v1` nếu project đang dùng.
  - `ApiResponse` / error handler hiện có nếu đã tồn tại.
  - Không return entity trực tiếp nếu project dùng DTO.
  - Không đưa business logic vào controller.
  - Controller gọi service, service xử lý nghiệp vụ, repository chỉ truy vấn.

### Deliverables

- Global response/error format.
- Security foundation.
- Swagger/OpenAPI.
- Logging/correlation ID.
- BaseEntity/common audit fields.

---

## PHASE 1 - DATABASE MIGRATION

### Tương ứng trong SRS

- Mục 5.2. Database migration và seed data.
- Liên quan toàn bộ entity của các module:
  - Auth/user.
  - Movie/genre/actor.
  - Cinema/room/seat/showtime.
  - Booking/ticket/payment/refund.
  - Loyalty/notification/review/recommendation/upload/audit.

### Trạng thái hiện tại

**HOÀN THÀNH MỘT PHẦN**.

### Vấn đề hiện tại

- Có migration version nhưng Flyway đang tắt.
- Runtime dùng PostgreSQL nhưng migration cũ có dấu hiệu dùng dialect khác.
- Production vẫn đang có nguy cơ phụ thuộc `ddl-auto=update`.

### Cần làm nếu chỉnh migration

- Chuyển migration sang PostgreSQL.
- Bật Flyway cho môi trường production.
- Production nên dùng `spring.jpa.hibernate.ddl-auto=validate`.
- Seed tối thiểu:
  - Admin account.
  - Role mặc định.
  - Một rạp duy nhất.
  - Phòng/ghế mẫu nếu cần demo.

### Bổ sung cho Refund

Nếu triển khai refund theo prompt mới, migration/schema cần bao phủ:

- `cinema_incidents`
- `refunds`
- `refund_claims`
- `notifications` nếu chưa đủ field
- enum/status tương ứng
- index cho:
  - `booking_id`
  - `payment_id`
  - `incident_id`
  - `status`
  - `room_id`
  - `started_at`
  - `ended_at`
  - `user_id`
  - `created_at`

---

## PHASE 2 - AUTH, CUSTOMER & SECURITY

### Tương ứng trong SRS

- Mục 3. Actors và phân quyền.
- Mục 5.3. Xác thực, tài khoản và hồ sơ người dùng.
- Mục 6. NFR Security.

### Tương ứng trong ROLE_BASED_BUSINESS_FLOWS

- GUEST đăng ký, đăng nhập, reset mật khẩu.
- CUSTOMER quản lý hồ sơ.
- STAFF đăng nhập bằng tài khoản được ADMIN cấp.
- ADMIN quản lý tài khoản CUSTOMER/STAFF.

### Trạng thái hiện tại

**ĐÃ HOÀN THÀNH**.

### Ghi chú quan trọng

- Role khách hàng là `CUSTOMER`, không nên đặt tên nghiệp vụ là `USER` trong tài liệu code mới nếu project đang dùng `CUSTOMER`.
- API `/api/v1/admin/**` dành cho `ADMIN`.
- API `/api/v1/staff/**` theo nghiệp vụ hiện tại dành cho `STAFF`.
- Theo role flow hiện tại, `ADMIN` không có quyền check-in vé; check-in là nghiệp vụ của `STAFF`.

### Liên quan Refund

- CUSTOMER được xem refund proof, refund history, notification của chính mình.
- CUSTOMER được tạo missing refund claim nếu booking thuộc sự cố nhưng chưa được refund.
- ADMIN xử lý refund theo SRS hiện tại.
- Nếu muốn STAFF cũng tạo incident/refund như prompt mới thì đây là **scope change** cần cập nhật lại role flow và security rule.

---

## PHASE 3 - MOVIE, GENRE & ACTOR MANAGEMENT

### Tương ứng trong SRS

- Mục 5.4. Phim, thể loại và diễn viên.
- Liên quan gián tiếp tới showtime, booking, review, recommendation và report.

### Trạng thái hiện tại

**ĐÃ HOÀN THÀNH**.

### Ghi chú mapping

Movie là dữ liệu gốc cho:

```text
Movie
  -> Showtime
  -> Booking
  -> Payment/Refund
  -> Reports
  -> Recommendation
```

### Liên quan Refund

- Refund history có thể hiển thị `movieTitle` thông qua `Booking -> Showtime -> Movie`.
- Refund report/history cho staff/admin có thể filter theo phim nếu mở rộng.

---

## PHASE 4 - CINEMA, ROOM, SEAT & SHOWTIME

### Tương ứng trong SRS

- Mục 5.5. Rạp, phòng và sơ đồ ghế.
- Mục 5.6. Suất chiếu.
- Quy tắc single-cinema ở mục 2.1.

### Trạng thái hiện tại

**HOÀN THÀNH MỘT PHẦN**.

### Đã có

- Quản lý rạp duy nhất theo đúng phạm vi: GET/UPDATE/PATCH status, không CREATE/DELETE rạp.
- Admin quản lý phòng, ghế, layout.
- Admin tạo/cập nhật/bulk suất chiếu.
- Kiểm tra xung đột phòng/thời gian.
- Chặn hủy suất nếu đã có booking.

### Còn thiếu

- Scheduler tự chuyển `SCHEDULED -> OPEN -> COMPLETED`.

### Liên quan Refund

Refund incident phụ thuộc Phase 4 vì cần:

- Phòng chiếu bị sự cố.
- Khoảng thời gian sự cố.
- Tìm suất chiếu bị ảnh hưởng bằng điều kiện giao thời gian:

```text
showtime.room.id = incident.room.id
AND showtime.startTime < incident.endedAt
AND showtime.endTime > incident.startedAt
```

### Bổ sung nghiệp vụ mới nếu triển khai incident refund

Cần thêm hoặc hoàn thiện:

- `CinemaIncident`
- `IncidentType`
- `IncidentStatus`
- API tạo incident.
- Query chống trùng incident confirmed cùng phòng và giao thời gian.
- Có thể bổ sung trạng thái showtime `INTERRUPTED` nếu code hiện tại hỗ trợ hoặc được phép mở rộng.

---

## PHASE 5 - BOOKING, SEAT LOCKING, F&B & TICKET QR

### Tương ứng trong SRS

- Mục 5.7. Giá vé.
- Mục 5.8. Đồ ăn và combo.
- Mục 5.9. Booking và giữ ghế.
- Mục 5.12. Vé QR và check-in staff.

### Tương ứng trong ROLE_BASED_BUSINESS_FLOWS

- CUSTOMER hold ghế, tạo booking, thanh toán, xem vé.
- STAFF check-in booking bằng QR.

### Trạng thái hiện tại

**HOÀN THÀNH MỘT PHẦN**.

### Đã có

- Booking core.
- Hold ghế.
- Tính giá vé/F&B.
- Sinh QR sau payment.
- API check-in nền tảng.

### Còn thiếu/rủi ro

- Database lock hoặc unique constraint chống double booking.
- Staff FE còn mock, chưa tích hợp API thật đầy đủ.
- QR chưa có chữ ký/chống giả mạo.

### Liên quan Refund

Refund phụ thuộc Phase 5 vì cần:

- Booking phải `PAID` mới đủ điều kiện refund.
- Khi refund thành công, booking đổi trạng thái phù hợp.
- BookingSeat/Ticket có thể cần chuyển trạng thái cancel/refund nếu project có trạng thái riêng.
- Refund history/proof lấy thông tin từ booking, ghế, suất chiếu, phim, payment.

### Lưu ý conflict nghiệp vụ

SRS hiện có luồng refund do CUSTOMER yêu cầu theo chính sách thông thường:

```text
PAID -> REFUND_REQUESTED -> REFUNDED
```

Prompt refund mới là luồng khác:

```text
PAID -> CANCELLED_REFUNDED hoặc REFUNDED do CINEMA_INCIDENT
```

Vì vậy phải tách rõ 2 loại refund:

1. **Customer policy refund**
   - CUSTOMER yêu cầu refund.
   - Có thể kiểm tra chưa check-in và còn trước giờ chiếu theo chính sách.
   - Thuộc scope SRS hiện tại.

2. **Cinema incident refund**
   - Do sự cố phòng chiếu.
   - Không phải user tự hủy vé.
   - Không quan tâm user đã check-in hay chưa.
   - Không kiểm tra điều kiện trước giờ chiếu 2 tiếng.
   - Refund toàn bộ booking PAID bị ảnh hưởng.
   - Đây là scope mở rộng mới của Phase 6.

---

## PHASE 6 - PAYMENT & REFUND

### Tương ứng trong SRS

- Mục 5.11. Thanh toán và hoàn tiền.
- Mục PAY-01 đến PAY-10.
- Mục 7. Ma trận trạng thái module: `Phase 6 | Payment & Refund`.

### Tương ứng trong ROLE_BASED_BUSINESS_FLOWS

- CUSTOMER-03: Thanh toán booking.
- CUSTOMER-05: Hủy booking và yêu cầu refund.
- ADMIN-07: Quản lý booking/refund.
- SYSTEM-03: Xử lý callback thanh toán.
- VNPAY-01: Thanh toán.
- VNPAY-02: Refund.

### Trạng thái hiện tại

**HOÀN THÀNH MỘT PHẦN**.

### Đã có

- VNPay payment sandbox/core.
- Mock payment cho dev/demo.
- Payment callback return/IPN.
- Idempotency cơ bản khi payment callback lặp.
- Một phần refund request/admin refund thủ công.

### Còn thiếu theo SRS hiện tại

- UI vận hành refund hoàn chỉnh.
- VNPay Refund API tự động.
- Đối soát/báo cáo giao dịch.
- Loyalty ledger để hoàn/trừ điểm chính xác khi refund.
- Notification tự động từ payment/refund.

---

### 6.A. Customer policy refund - luồng SRS hiện tại

Đây là luồng refund do CUSTOMER chủ động yêu cầu theo chính sách vận hành.

#### Actor

- CUSTOMER
- ADMIN
- SYSTEM
- VNPAY nếu tích hợp thật

#### Rule chính

- CUSTOMER chỉ gửi yêu cầu refund cho booking của chính mình.
- Booking phải `PAID`.
- Vé chưa check-in/booking chưa `USED`.
- Chưa refund trước đó.
- Còn trước giờ chiếu tối thiểu theo chính sách vận hành.
- Booking `USED`, `REFUNDED`, `CANCELLED`, `EXPIRED` không được refund lại.

#### Trạng thái

```text
PAID -> REFUND_REQUESTED -> REFUNDED
```

#### Main phase

- Main: Phase 6.
- Dependency: Phase 5.
- Related: Phase 7 loyalty/notification, Phase 12 QA.

---

### 6.B. Cinema incident refund - luồng mở rộng mới

Đây là luồng refund do phòng chiếu gặp sự cố, ví dụ mất điện, lỗi máy chiếu, lỗi âm thanh, lỗi điều hòa hoặc sự cố khác làm ảnh hưởng suất chiếu.

#### Main phase

**PHASE 6 - PAYMENT & REFUND**.

#### Dependency phase

- Phase 4: phòng chiếu, suất chiếu, incident.
- Phase 5: booking, booking seat/ticket, trạng thái booking.

#### Related phase

- Phase 7: notification và loyalty.
- Phase 9: staff/admin operation, audit log, reports.
- Phase 10: WebSocket realtime optional.
- Phase 12: integration test và QA.

#### Actor đề xuất

Theo SRS/role flow hiện tại:

- ADMIN là actor chính xử lý booking/refund.
- STAFF hiện chỉ vận hành check-in.

Theo prompt mở rộng:

- STAFF/ADMIN có thể tạo incident và trigger refund.

**Quyết định cần chốt:**

- Nếu giữ đúng role flow hiện tại: chỉ `ADMIN` tạo incident/refund.
- Nếu muốn `STAFF` cũng xử lý incident/refund: phải cập nhật `ROLE_BASED_BUSINESS_FLOWS.md`, security config và API authorization.

#### Rule chính

- Đây không phải luồng CUSTOMER tự hủy vé.
- Refund do sự cố phòng chiếu.
- Hệ thống tìm showtime bị ảnh hưởng theo room + khoảng thời gian.
- Hệ thống tìm booking `PAID` thuộc các showtime bị ảnh hưởng.
- Refund 100% payment amount.
- Không quan tâm CUSTOMER đã check-in hay chưa.
- Không kiểm tra trước giờ chiếu 2 tiếng.
- Không yêu cầu CUSTOMER nhập tài khoản ngân hàng.
- Không làm staff chuyển khoản thủ công.
- Không upload ảnh chuyển khoản làm proof chính.
- Sandbox/demo dùng `MockPaymentGatewayRefundService`.
- Bằng chứng refund là `gatewayRefundId`, `gatewayTransactionId`, amount, refundedAt, gatewayMessage.
- Sandbox phải ghi rõ: `Sandbox refund completed. No real money was transferred.`

#### Entity/enum nên có

- `CinemaIncident`
- `Refund`
- `RefundClaim`
- `Payment`
- `Notification`
- `IncidentType`
- `IncidentStatus`
- `RefundStatus`
- `RefundTrigger`
- `RefundProofType`
- `RefundClaimStatus`
- `PaymentStatus`

#### API chính

Incident:

```text
POST /api/v1/admin/incidents
POST /api/v1/admin/incidents/{incidentId}/refunds
GET  /api/v1/admin/incidents/{incidentId}/refunds
GET  /api/v1/admin/incidents/{incidentId}/refund-history
```

Nếu chốt STAFF cũng được xử lý thì có thể dùng `/api/v1/staff/incidents/**`, nhưng phải cập nhật role flow trước.

Customer claim/proof/history:

```text
POST /api/v1/bookings/{bookingId}/refund-claims
GET  /api/v1/me/refund-claims
GET  /api/v1/bookings/{bookingId}/refund-proof
GET  /api/v1/me/refunds/history
GET  /api/v1/bookings/{bookingId}/refund-history
```

Admin/staff claim/history:

```text
GET   /api/v1/admin/refund-claims?status=PENDING
PATCH /api/v1/admin/refund-claims/{claimId}/approve
PATCH /api/v1/admin/refund-claims/{claimId}/reject
GET   /api/v1/admin/refunds/history
GET   /api/v1/admin/refund-claims/history
```

Notification:

```text
GET   /api/v1/me/notifications
PATCH /api/v1/me/notifications/{notificationId}/read
GET   /api/v1/me/notifications/unread-count
```

#### Trạng thái sau refund success

- `Refund.status = REFUNDED`
- `Refund.gatewayRefundId != null`
- `Refund.proofType = GATEWAY_REFERENCE`
- `Refund.proofReference = gatewayRefundId`
- `Refund.refundedAt != null`
- `Payment.status = REFUNDED`
- `Booking.status = CANCELLED_REFUNDED` hoặc trạng thái tương ứng của project
- `CinemaIncident.refundProcessed = true`
- CUSTOMER nhận notification refund completed

#### Trạng thái khi refund failed

- `Refund.status = FAILED`
- `Refund.gatewayMessage = reason failed`
- `Payment` vẫn giữ `PAID`
- `Booking` vẫn giữ `PAID`
- Không set `Booking` sang `CANCELLED_REFUNDED`
- Không set `Payment` sang `REFUNDED`
- Không có proof refunded hợp lệ
- ADMIN/STAFF nhận notification refund failed nếu có quyền nhận

#### Kết luận mapping Phase 6

Refund nằm chính ở Phase 6, nhưng cần chia task nhỏ để AI triển khai:

```text
Refund Phase 1: Enum + Entity + Repository
Refund Phase 2: CinemaIncident create API
Refund Phase 3: Incident-based refund processing
Refund Phase 4: RefundClaim create/approve/reject
Refund Phase 5: RefundProof + RefundHistory API
Refund Phase 6: Notification API/polling, realtime optional
Refund Phase 7: Audit log + build/test + QA checklist
```

---

## PHASE 7 - LOYALTY & NOTIFICATION

### Tương ứng trong SRS

- Mục 5.13. Loyalty.
- Mục 5.14. Notification.

### Tương ứng trong ROLE_BASED_BUSINESS_FLOWS

- CUSTOMER-07: Loyalty.
- SYSTEM-04: Loyalty ledger.
- SYSTEM-05: Notification tự động.

### Trạng thái hiện tại

**HOÀN THÀNH MỘT PHẦN**.

### Còn thiếu

- Loyalty ledger chi tiết.
- Đổi điểm khi thanh toán.
- Hoàn/trừ điểm chính xác khi booking refund.
- Notification event-driven từ payment, cancel, refund, showtime.
- UI notification.
- Mark all read.
- WebSocket realtime notification.

### Liên quan Refund

Refund cần Phase 7 cho:

- Tạo notification khi incident ảnh hưởng booking.
- Tạo notification khi refund completed/failed.
- Tạo notification khi CUSTOMER tạo missing refund claim.
- Tạo notification khi ADMIN/STAFF approve/reject claim.
- Hoàn/trừ loyalty point khi booking được refund.

### Notification type gợi ý cho refund

- `CINEMA_INCIDENT_CREATED`
- `INCIDENT_REFUND_STARTED`
- `INCIDENT_REFUND_COMPLETED`
- `INCIDENT_REFUND_FAILED`
- `REFUND_CLAIM_CREATED`
- `REFUND_CLAIM_APPROVED`
- `REFUND_CLAIM_REJECTED`

---

## PHASE 8 - REVIEW

### Tương ứng trong SRS

- Mục 5.15. Review phim.

### Trạng thái hiện tại

**CHƯA THỰC HIỆN**.

### Ghi chú mapping

- Review cần booking `USED`.
- Mỗi CUSTOMER chỉ review một lần cho mỗi phim.
- Review được duyệt mới public và tính vào điểm trung bình.

### Liên quan Refund

Không phụ thuộc trực tiếp vào refund, nhưng có rule nghiệp vụ:

- Booking đã refund/cancel không nên được dùng làm điều kiện review.
- Booking `USED` nhưng bị incident refund cần chốt lại rule: có cho review hay không.

---

## PHASE 9 - STAFF OPERATIONS, AUDIT & REPORTS

### Tương ứng trong SRS

- Mục 5.19. Staff operations và báo cáo.
- Mục 9.1/P0-P2 trong danh sách ưu tiên nếu có liên quan staff/report.

### Tương ứng trong ROLE_BASED_BUSINESS_FLOWS

- STAFF-01: Đăng nhập staff.
- STAFF-02: Check-in booking bằng QR.
- STAFF-03: Tra cứu booking thủ công.
- ADMIN-10: Báo cáo.

### Trạng thái hiện tại

**CHƯA THỰC HIỆN** ở mức tổng thể staff ops/report.

### Lưu ý role quan trọng

- STAFF theo role flow hiện tại chỉ vận hành check-in.
- ADMIN không có quyền check-in.
- Nếu mở rộng STAFF xử lý incident/refund thì phải cập nhật role flow/security rõ ràng.

### Liên quan Refund

Phase 9 hỗ trợ refund bằng:

- Audit log cho incident/refund/claim.
- Refund report/history cho admin.
- Ghi nhận actor xử lý refund.
- Dashboard/report doanh thu sau refund nếu mở rộng.

### Audit action nên có cho refund

- `INCIDENT_CREATED`
- `INCIDENT_REFUND_STARTED`
- `INCIDENT_REFUND_COMPLETED`
- `INCIDENT_REFUND_FAILED`
- `REFUND_CLAIM_CREATED`
- `REFUND_CLAIM_APPROVED`
- `REFUND_CLAIM_REJECTED`
- `USER_VIEW_REFUND_HISTORY` optional
- `STAFF_VIEW_REFUND_HISTORY` optional
- `USER_VIEW_REFUND_PROOF` optional
- `NOTIFICATION_SENT` optional

---

## PHASE 10 - STORAGE, EMAIL, WEBSOCKET & SCHEDULER

### Tương ứng trong SRS

- Mục 5.18. Upload và lưu trữ.
- Mục 5.12. Vé QR và check-in staff, phần gửi vé qua email.
- Mục 5.14. Notification, phần WebSocket.
- SHOW-08 scheduler trạng thái suất chiếu.
- BOOK-09 scheduler giải phóng hold.

### Tương ứng trong ROLE_BASED_BUSINESS_FLOWS

- SYSTEM-01: Giải phóng hold hết hạn.
- SYSTEM-02: Cập nhật trạng thái suất chiếu.
- SMTP-01: Email xác thực và vé.
- CLOUDINARY-01: Upload ảnh.

### Trạng thái hiện tại

**HOÀN THÀNH MỘT PHẦN**.

### Còn thiếu

- Email vé QR sau thanh toán.
- WebSocket seat map.
- WebSocket notification.
- Scheduler tự chuyển trạng thái suất chiếu.
- UI quản lý file upload/xóa Cloudinary.

### Liên quan Refund

- WebSocket notification cho refund là optional.
- Nếu chưa có WebSocket, phải có Notification API để frontend polling.
- Không được để lỗi push realtime làm rollback transaction refund.
- Email cancel/refund có thể là future enhancement.

---

## PHASE 11 - RECOMMENDATION & AI ANALYSIS

### Tương ứng trong SRS

- Mục 5.16. Recommendation cá nhân hóa.
- Mục 5.17. AI phân tích nội dung phim.

### Trạng thái hiện tại

**HOÀN THÀNH MỘT PHẦN**.

### Ghi chú mapping

- AI analysis admin workflow tốt hơn recommendation end-user.
- Recommendation cần dữ liệu từ trailer interaction, booking, review và user profile.

### Liên quan Refund

- Booking bị refund có thể cần giảm trọng số trong recommendation nếu project dùng booking history làm tín hiệu sở thích.
- Không bắt buộc trong MVP refund.

---

## PHASE 12 - INTEGRATION & QA

### Tương ứng trong SRS

- Mục 6. Yêu cầu phi chức năng.
- Mục 9. Danh sách chưa hoàn thành hoặc cần làm tiếp.
- Mục 10. Tiêu chí nghiệm thu tổng thể.
- Mục 11. Kết quả xác minh.

### Trạng thái hiện tại

**HOÀN THÀNH MỘT PHẦN**.

### Cần làm

- API contract đầy đủ.
- Swagger cleanup.
- Postman collection.
- E2E test.
- Security/RBAC test.
- Negative test.
- Test concurrency booking.
- Test payment callback idempotency.
- Test booking/ticket/payment/refund state machine.

### Refund QA checklist

Cần test tối thiểu:

1. Tạo incident hợp lệ.
2. Chặn incident trùng phòng và giao thời gian.
3. Tìm đúng showtime bị ảnh hưởng.
4. Refund đúng booking `PAID`.
5. Không refund trùng booking.
6. Mock gateway success cập nhật `Refund`, `Payment`, `Booking` đúng.
7. Mock gateway failed không đổi `Payment`/`Booking` sang refunded.
8. CUSTOMER xem refund proof của chính mình.
9. CUSTOMER không xem được proof/history của người khác.
10. CUSTOMER tạo refund claim khi booking thuộc incident nhưng chưa refund.
11. ADMIN/STAFF approve/reject claim theo scope đã chốt.
12. Notification được lưu DB trước khi push realtime.
13. Sandbox message ghi rõ không có tiền thật được chuyển.
14. Không lộ số tài khoản/thẻ/OTP/thông tin ngân hàng.

---

## Tổng kết mapping nhanh

| Phase DOCX | Module cập nhật | Mục SRS/Role tương ứng | Trạng thái theo SRS hiện tại |
|---|---|---|---|
| Phase 0 | Shared Foundation | SRS 1, 2, 3, 4, 5.1, 6 | ĐÃ HOÀN THÀNH |
| Phase 1 | Database Migration | SRS 5.2 | HOÀN THÀNH MỘT PHẦN |
| Phase 2 | Auth, Customer & Security | SRS 5.3, Role GUEST/CUSTOMER/STAFF login | ĐÃ HOÀN THÀNH |
| Phase 3 | Movie, Genre & Actor | SRS 5.4 | ĐÃ HOÀN THÀNH |
| Phase 4 | Cinema, Room, Seat & Showtime | SRS 5.5, 5.6 | HOÀN THÀNH MỘT PHẦN |
| Phase 5 | Booking, Seat Locking, F&B & QR | SRS 5.7, 5.8, 5.9, 5.12 | HOÀN THÀNH MỘT PHẦN |
| Phase 6 | Payment & Refund | SRS 5.11, Role CUSTOMER-03/05, ADMIN-07, SYSTEM-03, VNPAY-02 | HOÀN THÀNH MỘT PHẦN |
| Phase 7 | Loyalty & Notification | SRS 5.13, 5.14, Role SYSTEM-04/05 | HOÀN THÀNH MỘT PHẦN |
| Phase 8 | Review | SRS 5.15 | CHƯA THỰC HIỆN |
| Phase 9 | Staff Operations, Audit & Reports | SRS 5.19, Role STAFF/ADMIN reports | CHƯA THỰC HIỆN |
| Phase 10 | Storage, Email, WebSocket & Scheduler | SRS 5.18, SHOW-08, NOTI-05, TICKET-07 | HOÀN THÀNH MỘT PHẦN |
| Phase 11 | Recommendation & AI Analysis | SRS 5.16, 5.17 | HOÀN THÀNH MỘT PHẦN |
| Phase 12 | Integration & QA | SRS 6, 9, 10, 11 | HOÀN THÀNH MỘT PHẦN |

---

## Ghi chú riêng cho Refund Feature

### Refund đặt ở phase nào?

Refund đặt chính ở:

```text
PHASE 6 - PAYMENT & REFUND
```

### Vì sao?

Refund phụ thuộc trực tiếp vào:

- `Payment`
- `payment.status`
- `gatewayTransactionId`
- `amount`
- payment gateway/mock gateway
- booking/payment state machine

### Phụ thuộc phase nào?

```text
Dependency:
- Phase 4: Cinema/Room/Showtime/Incident
- Phase 5: Booking/Seat/Ticket state

Related:
- Phase 7: Loyalty/Notification
- Phase 9: Audit/Reports/Operations
- Phase 10: WebSocket optional
- Phase 12: QA/E2E/RBAC
```

### Conflict cần chốt trước khi code

SRS hiện mô tả refund thông thường do CUSTOMER yêu cầu: booking phải PAID, chưa check-in và còn trước giờ chiếu theo chính sách.

Prompt mới mô tả refund do sự cố phòng chiếu: không quan tâm check-in, không kiểm tra điều kiện trước giờ chiếu, refund toàn bộ booking PAID bị ảnh hưởng.

Vì vậy khi giao cho AI/Codex phải ghi rõ:

```text
Implement Cinema Incident Refund, not Customer Self-Cancellation Refund.
```

Và nên tạo enum/field phân biệt trigger:

```text
RefundTrigger.CINEMA_INCIDENT
RefundTrigger.USER_MISSING_REFUND_CLAIM
```

Nếu sau này cần giữ luồng refund thông thường thì có thể thêm:

```text
RefundTrigger.CUSTOMER_POLICY_REQUEST
```

---

## Prompt chuẩn sau khi update file này

```md
Follow the project harness before implementing this task.

Read these files first:
- AGENTS.md
- architecture.md
- domain-rules.md
- api-contract.md
- feature_list.json
- progress.md
- phase_mapping_srs_vs_docx_refund_role_updated.md
- SRS_CINEMA_SYSTEM_COMPLETE.md
- ROLE_BASED_BUSINESS_FLOWS.md

Task:
Implement Phase 6 - Payment & Refund: Cinema Incident Refund.

Important:
- This is not CUSTOMER self-cancellation refund.
- This is incident-based refund for room/showtime incidents.
- Use role `CUSTOMER` instead of `USER` if the project uses CUSTOMER.
- Confirm whether STAFF is allowed to create incident/refund. If not updated in role flow/security, default to ADMIN only.
- Reuse existing Booking, Payment, Notification, AuditLog, Room/Showtime entities if they exist.
- Do not create duplicate entities.
- Follow Lombok style.
- Controller only calls service.
- Service handles business logic.
- Repository only queries data.
- Use PostgreSQL-compatible JPA mapping.
- Use MockPaymentGatewayRefundService for sandbox.
- Do not collect bank account numbers.
- Do not use transfer image as refund proof.
- Do not transfer real money in sandbox.

Before coding:
1. Summarize relevant rules from the harness.
2. Inspect existing codebase.
3. Identify impacted files.
4. Propose implementation plan by refund phases.
5. Wait for approval before implementing Phase 1.
```
