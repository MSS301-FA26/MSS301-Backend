# Mapping phase DOCX theo SRS_CINEMA_SYSTEM.md

Nguồn đối chiếu:
- DOCX ban đầu: `D:\FPTK8\SBA301\BE-LastUpdate.docx`
- SRS mới: `C:\Users\quyet\Downloads\SRS_CINEMA_SYSTEM.md`

Ghi chú: DOCX chia theo phase triển khai backend, còn file MD chia theo luồng nghiệp vụ. Vì vậy một số mục trong MD được gom vào cùng một phase DOCX.

## PHASE 0 - SHARED FOUNDATION

Tương ứng trong MD:
- Mục 1. Tổng quan dự án
- Mục 2. Mục tiêu hệ thống
- Mục 3. Phạm vi hệ thống
- Mục 4. Actors và vai trò
- Mục 20. Yêu cầu phi chức năng

Lý do mapping:
- Đây là nền tảng chung cho backend: kiến trúc REST API, actors, scope, bảo mật, hiệu năng và tính nhất quán dữ liệu.
- MD không tách riêng phase foundation, nhưng các mục đầu và NFR chính là nội dung nền tảng.

## PHASE 1 - DATABASE MIGRATION

Tương ứng trong MD:
- Không có mục riêng về Flyway/schema migration.
- Có liên quan gián tiếp đến tất cả các luồng nghiệp vụ có entity/trạng thái:
  - Mục 6. Rạp, phòng, ghế
  - Mục 7. Lịch chiếu
  - Mục 8. Booking
  - Mục 9. Ticket
  - Mục 10. Payment
  - Mục 12. Refund
  - Mục 13-14 và 16-19. Review, loyalty, recommendation, notification, upload, scheduler

Cần bổ sung nếu chia task lại:
- Schema versioning.
- Migration strategy.
- Seed data.
- Migration tool:
  - Flyway
  - hoặc Liquibase
- Cấu trúc migration:
  - `V1__init_schema.sql`
  - `V2__auth_tables.sql`
  - `V3__movie_tables.sql`
  - `V4__booking_tables.sql`
  - ...
- Tạo migration cho users/roles/tokens.
- Tạo migration cho movie/genre/actor/movie_actor.
- Tạo migration cho cinema/room/seat/showtime.
- Tạo migration cho booking/booking_seat/ticket/ticket_type/price_rule.
- Tạo migration cho payment/refund.
- Tạo migration cho loyalty/notification/review/recommendation/upload/audit/scheduler metadata.

Seed data nên có:
- Admin account.
- Default genres.
- Default cinema.
- Default room.

Deliverables:
- Database schema.
- Migration scripts.
- Seed scripts.

## PHASE 2 - AUTH, USER & SECURITY

Tương ứng trong MD:
- Mục 4. Actors và vai trò
- Mục 5. Luồng xác thực & quản lý tài khoản
  - 5.1 Đăng ký tài khoản
  - 5.2 Đăng nhập bằng email và mật khẩu
  - 5.3 Đăng nhập bằng Google OAuth
  - 5.4 Làm mới Access Token
  - 5.5 Quên mật khẩu
  - 5.6 Đổi mật khẩu
  - 5.7 Đăng xuất
  - 5.8 Admin quản lý tài khoản người dùng
- Mục 20.1 Bảo mật

Trạng thái theo MD:
- Đã hoàn thành cả FE và BE.

## PHASE 3 - MOVIE & GENRE MANAGEMENT

Tương ứng trong MD:
- Mục 3. Phạm vi hệ thống: khách hàng tìm kiếm phim, xem chi tiết phim/trailer.
- Mục 4. Actors: Guest xem danh sách phim, chi tiết phim, trailer.
- Mục 7. Lịch chiếu: Admin chọn phim để tạo suất chiếu.
- Mục 13. Review: review gắn với phim, điểm trung bình phim.
- Mục 16. Recommendation: dùng metadata phim, genre, đạo diễn, diễn viên.
- Mục 18. Upload & lưu trữ: poster phim.

Nên tách thành module riêng:
- Genre CRUD:
  - Create Genre
  - Update Genre
  - Delete Genre
  - List Genre
- Movie CRUD:
  - Create Movie
  - Update Movie
  - Delete Movie
  - Change Status
  - Movie Detail
  - Movie Search/filter

Movie Status:
- `COMING_SOON`
- `NOW_SHOWING`
- `ENDED`

Quan hệ:
- Movie N-N Genre.

Lý do nên ưu tiên:
- Movie là dữ liệu gốc của toàn hệ thống:

```text
Movie
 ↓
Showtime
 ↓
Booking
 ↓
Payment
 ↓
Reports
```

Khoảng trống của MD:
- MD không có mục riêng "Quản lý phim & thể loại".
- Movie xuất hiện ở showtime, booking, recommendation và reports nhưng chưa có module quản lý riêng.
- Nên bổ sung section riêng nếu muốn SRS đầy đủ:
  - CRUD phim.
  - CRUD genre.
  - Actor/MovieActor.
  - Poster, trailer, age rating, duration, director, cast, description.
  - Public movie list/search/filter/detail.

## PHASE 4 - CINEMA, ROOM, SEAT & SHOWTIME

Tương ứng trong MD:
- Mục 6. Luồng quản lý rạp, phòng, ghế
  - 6.1 Quy tắc đơn rạp
  - 6.2 Tạo và cập nhật rạp
  - 6.3 Tạo và quản lý phòng chiếu
  - 6.4 Sinh và quản lý sơ đồ ghế
- Mục 7. Luồng quản lý lịch chiếu
  - 7.1 Tạo suất chiếu đơn
  - 7.2 Tạo bulk suất chiếu
  - 7.3 Cập nhật suất chiếu
  - 7.4 Hủy suất chiếu
  - 7.5 Tra cứu sơ đồ ghế của suất chiếu

Trạng thái theo MD:
- Mục 6 và mục 7 đã hoàn thành cả FE và BE.

## PHASE 5 - BOOKING, SEAT LOCKING, F&B & TICKET QR

Tương ứng trong MD:
- Mục 8. Luồng đặt vé
  - 8.1 Tổng quan vòng đời đặt vé
  - 8.2 Hold ghế
  - 8.3 Tạo booking chính thức
  - 8.4 Hết hạn hold tự động
- Mục 9. Luồng vé (Ticket)
  - 9.1 Khái niệm vé
  - 9.2 Kiểm tra tuổi và loại vé
  - 9.3 Quy tắc giá vé
  - 9.4 Sinh mã QR vé
  - 9.5 Gửi vé qua email
  - 9.6 Trạng thái vé
- Mục 11. Staff xác nhận vé tại rạp
  - Phần QR/check-in liên quan trực tiếp đến Ticket QR.
- Mục 12. Hủy booking/refund
  - Phần chuyển trạng thái booking khi hủy booking hoặc hủy suất.
- Mục 19.1 Dọn hold ghế hết hạn

Trạng thái theo MD:
- Booking core, giá vé, QR/check-in nền tảng đã có.
- Staff check-in cần hoàn thiện thêm theo đặc tả mục 11.

## PHASE 6 - PAYMENT

Tương ứng trong MD:
- Mục 10. Luồng thanh toán
  - 10.1 Khởi tạo thanh toán
  - 10.2 Thanh toán qua VNPay
  - 10.3 Mock Payment
  - 10.4 Tra cứu trạng thái thanh toán
- Mục 12. Luồng hủy suất & hoàn tiền
  - 12.2 Người dùng gửi yêu cầu hoàn tiền
  - 12.3 Admin xét duyệt và xác nhận hoàn tiền thủ công
  - 12.4 Admin hủy suất chiếu
  - 12.5 Tóm tắt chuyển trạng thái booking trong luồng hủy suất

Trạng thái theo MD:
- VNPay và mock payment đã hoàn thiện cốt lõi với idempotency cơ bản.
- Refund cấp provider/VNPay Refund API tự động còn cần phát triển.

## PHASE 7 - LOYALTY & NOTIFICATION

Tương ứng trong MD:
- Mục 14. Luồng loyalty & tích điểm
  - 14.1 Tích điểm sau booking
  - 14.2 Xem điểm hiện tại
  - 14.3 Admin quản lý điểm
  - 14.4 Quy đổi điểm sang ưu đãi
- Mục 17. Luồng thông báo
  - 17.1 Tạo thông báo
  - 17.2 Người dùng xem thông báo
  - 17.3 Realtime notification

Trạng thái theo MD:
- Loyalty và notification đã có phần chính.
- Quy đổi điểm sang ưu đãi và realtime notification cần phát triển thêm.

Không nên làm lúc này:
- Email marketing:
  - Gửi khuyến mãi
  - Newsletter
  - Birthday email
- Loyalty nâng cao:
  - Rank Silver
  - Gold
  - Platinum

## PHASE 8 - REVIEW

Tương ứng trong MD:
- Mục 13. Luồng đánh giá phim
  - 13.1 Điều kiện viết đánh giá
  - 13.2 Tạo đánh giá
  - 13.3 Kiểm duyệt đánh giá (Admin)
  - 13.4 Cập nhật điểm trung bình phim
  - 13.5 Đánh giá ảnh hưởng đến recommendation

Trạng thái theo MD:
- Review end-to-end cần bổ sung ReviewController, ReviewService, API tạo/sửa review và API public list review theo phim.

## PHASE 9 - STAFF OPERATIONS, AUDIT & REPORTS

Tương ứng trong MD:
- Mục 11. Staff xác nhận vé tại rạp
  - 11.1 Mục đích và phạm vi
  - 11.2 Điều kiện tiên quyết
  - 11.3 Luồng xác nhận vé bằng QR
  - 11.4 Tra cứu vé thủ công
  - 11.5 Các trường hợp lỗi
  - 11.6 Quyền của Admin trong check-in
  - 11.7 Trạng thái booking sau check-in
- Mục 21. Cần phát triển thêm
  - Báo cáo Admin: doanh thu, tỷ lệ lấp đầy phòng chiếu, hiệu quả phim.

Staff Operations:
- Staff check-in vé bằng QR.
- Staff check-in bằng booking code.
- Staff xác nhận khách đã nhận combo.
- Staff xem booking theo suất chiếu.

Staff Ticket Check-in:
- Scan QR.
- Validate ticket.
- Mark checked-in.

Combo Pickup:
- Staff xác nhận khách nhận combo.

Audit Log:
- Log tạo/sửa/xóa phim.
- Log tạo/sửa suất chiếu.
- Log thay đổi trạng thái booking.
- Log check-in vé.
- Log xác nhận combo.
- Action nên log:
  - `CREATE_MOVIE`
  - `UPDATE_MOVIE`
  - `DELETE_SHOWTIME`
  - `CHECK_IN_TICKET`
  - `CONFIRM_COMBO`

Reports:
- Doanh thu theo ngày/tháng.
- Tổng vé bán.
- Phim bán chạy.
- Combo bán chạy.
- Doanh thu theo suất chiếu.
- Basic reports nên có:
  - Revenue Report.
  - Top Movies.
  - Top Combos.
  - Tickets Sold.

Không bắt buộc, có thể ghi là Future Enhancement:
- Staff Shift Management:
  - Ca sáng
  - Ca chiều
  - Ca tối
  - Check-in nhân viên
  - Check-out nhân viên
  - Đi trễ
  - Tính công
- Payroll:
  - Tính lương
  - OT
  - Phụ cấp
  - Khấu trừ
- Leave Request:
  - Xin nghỉ phép
  - Duyệt nghỉ phép
- HR Dashboard:
  - Hiệu suất nhân viên
  - Ngày công
  - Ca làm

Lý do loại khỏi scope:
- Đây là hệ thống HR, không phải hệ thống đặt vé phim.
- Payroll, leave request và HR dashboard không liên quan trực tiếp tới booking.

Khoảng trống của MD:
- MD có staff check-in và có nhắc báo cáo Admin, nhưng chưa có mục riêng hoặc chưa đủ chi tiết cho:
  - Staff xác nhận khách đã nhận combo/F&B tại rạp.
  - Audit log.
  - Reports cơ bản theo doanh thu, vé bán, phim bán chạy, combo bán chạy và doanh thu theo suất chiếu.

## PHASE 10 - STORAGE, EMAIL, WEBSOCKET & SCHEDULER

Tương ứng trong MD:
- Storage:
  - Mục 18. Upload & lưu trữ
  - 18.1 Upload hình ảnh
  - 18.2 Quản lý tài nguyên đã upload
- Email:
  - Mục 9.5 Gửi vé qua email
  - Mục 5 có liên quan email OTP/quen mật khẩu
- WebSocket:
  - Mục 17.3 Realtime notification
  - Mục 21: WebSocket realtime cập nhật trạng thái ghế và notification cần triển khai
- Scheduler:
  - Mục 19. Scheduler & dọn dẹp tự động
  - 19.1 Dọn hold ghế hết hạn
  - 19.2 Cập nhật trạng thái suất chiếu tự động

Nên làm nếu có thời gian:
- Email Service:
  - Booking Confirmation:
    - Movie.
    - Showtime.
    - Seats.
    - Combo.
    - Booking Code.
  - Ticket Email:
    - QR Code.
    - Ticket Information.
  - Cancel Email:
    - Booking cancelled.
    - Refund status.
- WebSocket:
  - Mục tiêu: realtime seat status.
  - Seat States:
    - `AVAILABLE`
    - `HELD`
    - `BOOKED`
  - Flow:
    - Select seat → `HELD`
    - Payment success → `BOOKED`
    - Timeout → `AVAILABLE`
  - Broadcast:
    - `Seat updated`
    - Gửi cho tất cả user đang xem cùng suất chiếu.

Lý do nên làm:
- Đây là điểm cộng lớn khi demo web đặt vé phim.

Trạng thái theo MD:
- Scheduler dọn hold ghế hết hạn đã có trong flow.
- Email vé đã được mô tả ở mục 9.5.
- WebSocket realtime cập nhật trạng thái ghế còn cần phát triển thêm.
- Notification cho user/admin có thể để sau nếu chưa cần trong MVP.
- Scheduler cập nhật trạng thái suất chiếu còn cần phát triển thêm.

## PHASE 11 - AI PERSONALIZED RECOMMENDATION

Tương ứng trong MD:
- Mục 13.5 Đánh giá ảnh hưởng đến recommendation.
- Mục 16. Luồng gợi ý phim cá nhân hóa
  - 16.1 Thu thập tín hiệu sở thích
  - 16.2 Hồ sơ sở thích người dùng
  - 16.3 Gợi ý phim
  - 16.4 Gợi ý theo diễn viên yêu thích
  - 16.5 Debug recommendation (Admin)

Trạng thái theo MD:
- Đã hoàn thiện mức rule-based với tín hiệu trailer, booking và review.

## PHASE 12 - INTEGRATION & QA

Tương ứng trong MD:
- Mục 20. Yêu cầu phi chức năng
  - 20.1 Bảo mật
  - 20.2 Tính nhất quán dữ liệu
  - 20.3 Hiệu năng
  - 20.4 Khả năng mở rộng
- Mục 21. Trạng thái hoàn thiện từng module
  - Đã hoàn thành tốt
  - Cần phát triển thêm

Ưu tiên triển khai cho web đặt vé phim:
- API contract.
- Postman collection.
- End-to-end test flow.
- Swagger cleanup.
- NFR checklist.

API Contract cần có:
- Request.
- Response.
- Error format.
- Authorization.
- Roles.

Postman Collection nên chia folder:
- Auth.
- Movies.
- Genres.
- Showtimes.
- Seats.
- Bookings.
- Payments.
- Combos.
- Staff.
- Reports.

Swagger Checklist:
- All APIs documented.
- Bearer JWT.
- Request examples.
- Response examples.
- Error responses.

Test Plan:
- Functional:
  - Login.
  - Booking.
  - Payment.
  - Check-in.
  - Combo Pickup.
- Security:
  - RBAC.
  - JWT.
  - Protected APIs.
- Integration:
  - Booking → Payment → Ticket → Email.
- Negative:
  - Invalid token.
  - Duplicate booking.
  - Expired payment.
  - Already checked-in ticket.

Các flow nên kiểm thử end-to-end:
- Auth.
- Cinema/room/seat.
- Movie & Genre.
- Showtime.
- Booking/hold seat.
- Payment.
- Ticket QR/check-in.
- Combo pickup.
- Refund/cancel showtime.
- Review/recommendation nếu nằm trong phạm vi bàn giao.

NFR checklist nên bao gồm:
- Security.
- Data consistency.
- Performance.
- Scalability.
- Transaction/locking.
- Payment callback idempotency.
- Booking/ticket/payment/refund state machine.

## Tổng kết mapping nhanh

| Phase DOCX | Mục MD tương ứng |
|---|---|
| Phase 0 - Shared Foundation | 1, 2, 3, 4, 20 |
| Phase 1 - Database Migration | Không có mục riêng; suy ra từ 5-19 |
| Phase 2 - Auth, User & Security | 4, 5, 20.1 |
| Phase 3 - Movie & Genre Management | 3, 4, 7, 13, 16, 18 |
| Phase 4 - Cinema, Room, Seat & Showtime | 6, 7 |
| Phase 5 - Booking, Seat Locking, F&B & Ticket QR | 8, 9, 11, 12, 19.1 |
| Phase 6 - Payment | 10, 12 |
| Phase 7 - Loyalty & Notification | 14, 17 |
| Phase 8 - Review | 13 |
| Phase 9 - Staff Operations, Audit & Reports | 11, ghi chú reports ở mục 21 |
| Phase 10 - Storage, Email, WebSocket & Scheduler | 5, 9.5, 17.3, 18, 19 |
| Phase 11 - AI Recommendation | 13.5, 16 |
| Phase 12 - Integration & QA | 20, 21 |

## Các phase được đổi

- AI Recommendation: từ Phase 4 thành Phase 11.
  - Lý do: AI cần dữ liệu từ booking, payment, review, user behavior và favorite genre. Nếu đặt quá sớm thì AI chỉ là demo.
- Loyalty & Notification: từ Phase 8 thành Phase 7.
  - Lý do: Loyalty phụ thuộc flow Booking → Payment → Earn Points.
- Review: từ Phase 9 thành Phase 8.
  - Lý do: Review phát sinh sau khi user xem phim và là dữ liệu đầu vào cho recommendation.

## Trạng thái đồng bộ với SRS

Các phần từng thiếu trong MD đã được bổ sung trực tiếp vào SRS_CINEMA_SYSTEM.md, không còn để riêng thành section phụ:

- Phase 1 - Database Migration: đã bổ sung vào 20.5 Database migration & seed data.
- Phase 3 - Movie & Genre Management: đã bổ sung vào 3.1 Quản lý phim & thể loại.
- Phase 9 - Staff Operations, Audit & Reports: đã bổ sung vào các mục 11.8 đến 11.12.
- Phase 10 - Email/WebSocket: đã bổ sung vào 9.5 Gửi vé qua email và 17.3 Realtime notification.
- Phase 12 - Integration & QA: đã bổ sung vào 20.6 Integration & QA checklist.
