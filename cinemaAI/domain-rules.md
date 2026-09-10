# CinemaAI Domain Rules

Nguồn sự thật ưu tiên: service implementation + enum + integration test. Tài liệu SRS chỉ được dùng bổ trợ khi không mâu thuẫn code.

## 1. Identity và authorization

- Role hợp lệ: `ADMIN`, `CUSTOMER`, `STAFF`.
- Email và phone phải duy nhất khi đăng ký/tạo staff.
- User disabled hoặc chưa verify email không được login thường.
- Password được BCrypt hash; đổi password yêu cầu old password đúng, confirm khớp và new password khác old password.
- Email verification OTP trong `AuthServiceImpl` có hiệu lực 90 giây.
- Email OTP service khác đang tạo OTP 5 phút; cần thống nhất nếu hai flow cùng được dùng.
- Access token mặc định 15 phút; refresh token 7 ngày.

## 2. Movie, actor, genre

- Movie title, actor name và genre name phải unique theo rule service/repository.
- Public không nhìn thấy movie `INACTIVE`; truy cập detail trả như không tồn tại.
- Chỉ movie `UPCOMING` được sửa nội dung bằng update flow hiện tại.
- Delete movie là soft delete sang `INACTIVE`.
- `mainActorIds` phải là tập con của `actorIds`.
- Age rating: `P`, `K`, `T13`, `T16`, `T18`.
- Ticket `CHILD` không được dùng cho phim rating 16+.

## 3. Cinema, room, seat

- Hệ thống giới hạn một cinema.
- Cinema/room mới mặc định `ACTIVE` nếu request không truyền status.
- Tên room phải unique trong cinema.
- Không generate ghế nếu room đã có ghế; replace chỉ dùng khi room đã có ghế.
- Row label và display order không được trùng trong một layout.
- Seat number không được trùng trong cùng row và không vượt column count của room.
- Couple seats phải có số lượng chẵn; cập nhật/xóa một ghế couple phải đồng bộ partner.
- Xóa ghế hiện là soft-disable sang `UNAVAILABLE`.

## 4. Showtime

- Status: `SCHEDULED -> OPEN -> COMPLETED`; `CANCELLED` là nhánh kết thúc.
- Movie `INACTIVE` và room không active không được tạo showtime.
- Start time phải ở tương lai; end time phải sau start time.
- Thời gian chiếm phòng = thời lượng phim + 15 phút cleanup.
- Không cho overlap showtime trong cùng room.
- Showtime mới chỉ nhận `SCHEDULED` hoặc `OPEN`.
- Không update showtime `CANCELLED`/`COMPLETED`.
- Không update thông tin showtime khi có active booking.
- Không complete trước thời điểm kết thúc.
- Hủy showtime xử lý booking:
  - `HOLDING`, `PENDING_PAYMENT` -> `CANCELLED`;
  - `PAID`, `USED` -> `REFUND_REQUESTED` -> VNPay/MOCK -> `REFUNDED` hoặc `REFUND_FAILED`;
  - terminal state khác được bỏ qua.
- Chỉ ADMIN/STAFF được hủy suất chiếu và kích hoạt hoàn tiền; CUSTOMER không được request refund booking đã thanh toán.
- Hoàn đúng `Booking.totalAmount`; loyalty chỉ điều chỉnh sau khi hoàn tiền thành công (VNPay mã `00` hoặc staff xác nhận thủ công).

## 5. Seat hold và booking

- Chỉ showtime `OPEN` được booking.
- Hold duration hiện tại: 3 phút.
- Mỗi customer chỉ được có một booking `HOLDING` hoặc `PENDING_PAYMENT` cho cùng một showtime. Muốn chọn ghế khác phải hủy booking đang giữ trước.
- Khi tạo hold, backend khóa pessimistic dòng showtime trước khi kiểm tra booking đang hoạt động để ngăn hai tab tạo hai đơn song song.
- `HOLDING`, `BOOKED`, `CHECKED_IN` là seat runtime status chặn hold mới.
- Seat phải thuộc room của showtime và có trạng thái physical available.
- Nếu ghế đã được hold/book bởi booking khác, trả `409 Conflict`.
- Booking state machine:

```text
HOLDING
  -> PENDING_PAYMENT
  -> PAID
  -> USED

HOLDING/PENDING_PAYMENT -> CANCELLED hoặc EXPIRED
PAID/USED -> REFUND_REQUESTED -> REFUNDED | REFUND_FAILED
```

- Create booking chỉ từ booking `HOLDING` chưa hết hạn.
- Số lượng ticket phải bằng số ghế đã hold.
- Nếu ticket line có `seatId`, quantity phải bằng 1.
- Ticket seat IDs phải unique, trùng đúng tập ghế hold và seat type phải khớp.
- Food selection phải chọn đúng một trong `foodItemId` hoặc `foodComboId`; quantity > 0; item/combo phải `ACTIVE`.
- Booking `USED` không được cancel.
- Chỉ booking `PAID` được check-in; QR nếu truyền phải khớp booking; sau check-in thành `USED`.
- Booking `REFUND_FAILED`: hệ thống gửi email thông báo cho khách; không có luồng hoàn tiền mặt/chuyển khoản thủ công trong API.
- Sau `REFUNDED`: `qrCode` và `paymentAccount` không còn hiển thị trong `BookingResponse`.

- Customer can create a standalone food order without a booking; the order belongs directly to that customer and is collected at the counter with `orderCode`.
- Food orders created from "My tickets" remain linked to an eligible `PAID`/`USED` booking; both standalone and linked orders use the same VNPay payment flow.
- A standalone concession order is displayed under "My food orders", never as a movie ticket.
- Online food orders remain `PENDING_PAYMENT` for 15 minutes from creation. The backend `expiresAt`
  value and VNPay `vnp_ExpireDate` must be identical.
- A failed or customer-cancelled VNPay attempt does not cancel the food order. The customer may
  retry payment or explicitly cancel the order before it expires.
- Expired unpaid food orders become `EXPIRED`; explicitly cancelled orders become `CANCELLED`;
  only `PAID` orders receive pickup entitlement and loyalty points.
- Paid food orders earn CinePoints using the active loyalty earning rate. Redemption is not enabled until food-order point reservation, cancellation and restoration are implemented safely.
- A paid standalone food order receives a `CINEAI:FOOD:<orderCode>:<customerId>` pickup QR. Staff must
  inspect the item list before confirming handoff. Confirmation is one-time: `PAID -> PICKED_UP`, records
  `pickedUpAt`, removes the active QR from customer responses, and a duplicate confirmation returns `409`.
- Ticket-linked concessions continue to use the movie-ticket workflow; the standalone pickup QR is only
  generated when `bookingId` is null.

## 6. Ticket pricing

- Ticket type/age:
  - `CHILD`: 0–12;
  - `STUDENT`: 13–25;
  - `ADULT`: 18–59.
- Viewer age phải thỏa ticket type và age rating của movie.
- Pricing rule active phải unique theo tổ hợp ticket type, room type, seat type, weekend và holiday.
- Late-night surcharge cố định hiện tại: 20.000 VND.
- Ticket combo phải có ít nhất một ticket.
- Combo name phải unique; inactive combo cùng tên vẫn tạo conflict có chủ đích.

Lưu ý: dải `STUDENT` và `ADULT` overlap 18–25; đây là rule hiện tại, không tự sửa nếu chưa có quyết định nghiệp vụ.

## 7. Payment

- Chỉ booking `HOLDING` hoặc `PENDING_PAYMENT` được khởi tạo/thực hiện payment.
- Khi retry thanh toán, payment `PENDING` cũ của cùng booking/food order được chuyển thành `FAILED` trước khi tạo phiên mới.
- Trước provider payment, `HOLDING` chuyển sang `PENDING_PAYMENT`.
- Payment success chỉ hoàn tất booking đang `PENDING_PAYMENT`.
- Success chuyển payment `SUCCESS`, booking `PAID`, seat `BOOKED`, ghi `paidAt`, sinh QR và kích hoạt side effects.
- Failure chuyển payment `FAILED`; không được giả lập success.
- VNPay callback phải kiểm tra chữ ký, amount và tránh xử lý lặp.
- Mock payment chỉ dành local/demo; phải disable hoặc bảo vệ trước production.

## 8. Loyalty và wallet

- Loyalty redemption points phải > 0 và không vượt balance.
- `POINTS_PER_UNIT = 10.000` trong service hiện tại; cần đọc method cụ thể trước khi đổi conversion semantics.
- Payment/refund và showtime cancellation có side effects loyalty/wallet; mọi thay đổi phải transaction-safe và idempotent.
- Ledger/refund provider hoàn chỉnh vẫn là khoảng trống cần theo dõi.

## 9. Review

- Chỉ customer có booking `USED` cho movie mới được review.
- Mỗi user chỉ có một review cho một movie.
- Review hidden/deleted không được customer update.
- Public list/average chỉ tính review được phép hiển thị.
- Admin có thể hide hoặc delete review.

## 10. Wishlist, recommendation, notification

- Wishlist unique theo user/movie.
- Recommendation thu thập trailer, booking, review và wishlist signals.
- Signal weights lấy từ `application.properties`; không hard-code lại ở controller.
- Notification user chỉ được đọc/mark-read notification thuộc mình.
- Endpoint tạo notification yêu cầu ADMIN.

## 11. Upload

- File ảnh là bắt buộc.
- Kích thước tối đa 5 MB.
- Chỉ JPG, PNG, WEBP.
- Cloudinary phải được cấu hình trước khi upload thật.

## 12. Reporting

- Revenue/ticket/occupancy chỉ dựa trên booking hợp lệ; repository hiện dùng `PAID` và `USED` trong khoảng `paidAt`.
- Date range mặc định và timezone phải được giữ nhất quán với service; API nhận ISO date.

## 13. Những rule cần xác nhận trước khi thay đổi

- Một cinema duy nhất hay multi-cinema.
- Customer có được request refund booking `PAID` hay không.
- Chính sách overlap `STUDENT`/`ADULT`.
- Thời hạn OTP 90 giây hay 5 phút.
- Conversion loyalty chính xác.
- Flyway là schema source of truth hay tiếp tục Hibernate `ddl-auto=update`.

