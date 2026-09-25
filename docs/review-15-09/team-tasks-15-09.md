# Phân công nhóm — Conceptual và Stitch ngày 15/09

## Deadline

- Mỗi thành viên gửi link/file có thể mở xem được trước tối 14/09.
- Ngày 15/09 chỉ cần Conceptual và Stitch, chưa cần code.
- Nếu đang bị chặn, báo ngay trong nhóm, không đợi sát deadline.

## Tuấn — Identity và Catalog

Hoàn thiện conceptual cho:

- User, Profile, Role.
- Movie, Genre, Actor.
- Cinema, Room, Seat và Showtime.
- Ticket type, ticket pricing.
- Food item và food combo.

Tuấn chốt với Khang các ID dùng để liên kết: userId, movieId, showtimeId, seatId, productId, bookingId và paymentId.

Luồng mua vé dùng bookingId làm ID chính. Không tạo thêm checkoutId hoặc orderId nếu chưa có nghiệp vụ riêng. foodOrderId chỉ dùng khi khách mua đồ ăn riêng.

Các ID giữa service chỉ là logical ID, không tạo foreign key sang database của service khác.

Khi checkout, Booking gửi một request sang Catalog để kiểm tra suất chiếu, ghế, loại vé và đồ ăn. Catalog trả giá có thẩm quyền cùng dữ liệu cần thiết để Booking lưu snapshot. Client chỉ gửi ID và số lượng; không gửi giá hoặc tổng tiền làm dữ liệu tin cậy.

## Khang — Booking và Payment

Hoàn thiện conceptual cho Booking, BookingSeat, BookingTicket, BookingFoodItem, FoodOrder, Payment, PaymentAttempt, Refund, seat hold, QR và check-in.

Luồng cần thống nhất:

1. Khách chọn suất chiếu và ghế.
2. Booking giữ ghế trong 3 phút.
3. Khách chọn loại vé và đồ ăn.
4. Booking lấy giá từ Catalog, tự tính tổng tiền và lưu snapshot.
5. Booking chuyển sang PENDING_PAYMENT.
6. Payment tạo giao dịch.
7. Thanh toán thành công: Booking chuyển PAID, ghế chuyển BOOKED và hệ thống tạo QR.
8. Thanh toán thất bại/hủy: giao diện nhận đúng trạng thái.
9. Hết thời gian giữ ghế: booking hết hạn và ghế được giải phóng.

Trạng thái cần chốt:

- Booking: HOLDING, PENDING_PAYMENT, PAID, USED, CANCELLED, EXPIRED, REFUND_REQUESTED, REFUNDED, REFUND_FAILED.
- Payment: PENDING, SUCCESS, FAILED, CANCELLED, REFUNDED.
- Seat runtime: HOLDING, BOOKED, RELEASED, CHECKED_IN.

Giao diện có thể dùng AVAILABLE, HELD, SOLD; Khang ghi rõ cách các nhãn này map sang trạng thái backend.

Snapshot cần chốt:

- Tên phim và poster nếu cần hiển thị trên vé.
- Tên rạp, phòng và thời gian chiếu.
- Nhãn ghế, loại ghế và giá từng ghế/vé.
- Tên đồ ăn/combo, đơn giá, số lượng và thành tiền.
- Giảm giá và tổng tiền.
- Thông tin khách hàng cần hiển thị trên vé/hóa đơn.

Mục tiêu là khi Catalog đổi tên hoặc đổi giá, đơn và vé đã mua vẫn hiển thị đúng dữ liệu tại thời điểm thanh toán.

## Khang bàn giao nghiệp vụ cho Vũ và Vy

Khang gửi cho Vũ và Vy:

- Trạng thái ghế và điều kiện chuyển trạng thái.
- Thời điểm bắt đầu countdown 3 phút.
- Cách xử lý ghế vừa bị người khác giữ và hold hết hạn.
- Trạng thái payment và hành động của giao diện.
- Cách lấy lại booking sau khi quay về từ cổng thanh toán.
- Nguồn lấy QR/vé và thông tin cần hiển thị.
- Chưa thanh toán thành công thì không hiển thị vé hợp lệ.

## Vũ — Stitch Web

Hoàn thiện phần còn thiếu theo core flow:

~~~text
Đăng nhập → Trang chủ → Chi tiết phim → Chọn suất
→ Chọn ghế → Chọn vé/đồ ăn → Checkout
→ Payment result → Lịch sử booking → Vé QR
~~~

Stitch cần có loading, empty, error; trạng thái ghế; countdown 3 phút; hold hết hạn; payment pending/success/failed/cancelled và QR sau khi thanh toán thành công.

Ưu tiên core flow, chưa cần dành thời gian cho animation hoặc màn hình phụ.

## Vy — Stitch Mobile

Thiết kế các màn hình:

- Login/register.
- Home và movie list/search.
- Movie detail và showtime selection.
- Seat selection.
- Ticket/food selection và checkout.
- Payment result.
- Booking history/detail và QR.

Mobile dùng chung màu sắc, font và phong cách của Web nhưng bố cục phải làm lại cho màn hình điện thoại. Trạng thái nghiệp vụ phải đồng bộ với Web và phần Khang bàn giao.

## Đức — Recommendation

Hoàn thiện conceptual:

~~~text
Interaction + Movie metadata
→ Content-based + Collaborative filtering
→ Adaptive hybrid
→ Danh sách phim đề xuất
~~~

Dữ liệu đầu vào dự kiến:

- userId, movieId.
- Thể loại, diễn viên, đạo diễn và metadata phim.
- Lượt xem/click phim.
- Wishlist.
- Booking đã thanh toán.
- Ticket đã check-in.
- Rating/review.

Đức chốt với Tuấn và Khang event/dữ liệu nào được gửi sang Recommendation, ID nào dùng để map, hệ thống nhận gì, xử lý gì và trả về gì.

Response tối thiểu gồm movieId, score và source/reason nếu có. Web/Mobile dùng movieId để lấy thông tin hiển thị từ Catalog.

## Leader — Tổng hợp và kiểm tra cuối

Sau khi nhận đủ:

- Ghép ba phần conceptual thành một bản chung.
- Kiểm tra mỗi loại dữ liệu chỉ có một service sở hữu.
- Kiểm tra không có foreign key hoặc truy vấn database xuyên service.
- Kiểm tra checkout, snapshot, payment và recommendation nối được với nhau.
- Đối chiếu trạng thái trong conceptual với Stitch Web/Mobile.
- Điền link Stitch vào README và mở thử bằng cửa sổ ẩn danh.
- Chuẩn bị phần giải thích khoảng 3 phút cho giảng viên.

## Mẫu bàn giao

Mỗi thành viên gửi:

~~~text
Đã hoàn thành:
Link/file:
Phần đã chốt với người liên quan:
Điểm còn chưa chắc hoặc đang bị chặn:
~~~
