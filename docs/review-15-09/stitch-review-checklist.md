# Stitch Review Checklist - 15/09

## 1. Design system dùng chung

- [ ] Web và mobile dùng cùng color palette, typography và icon style.
- [ ] Có button default/pressed/disabled/loading.
- [ ] Có input default/focus/error/disabled.
- [ ] Có Movie Card, Showtime Chip, Food Card và Status Badge.
- [ ] Có seat states: available, selected, holding, booked, unavailable, couple/VIP nếu có.
- [ ] Tên trạng thái khớp với conceptual.

## 2. Web core flow

- [ ] Home.
- [ ] Movie list/search/filter.
- [ ] Movie detail.
- [ ] Chọn ngày và showtime.
- [ ] Chọn ghế, có legend và countdown.
- [ ] Chọn ticket type.
- [ ] Chọn food/combo.
- [ ] Checkout có subtotal, discount và total.
- [ ] Payment success và failed.
- [ ] Booking history.
- [ ] Booking detail/QR ticket.

## 3. Flutter/mobile core flow

- [ ] Login/register.
- [ ] Home.
- [ ] Movie list/search.
- [ ] Movie detail.
- [ ] Showtime selection.
- [ ] Seat selection phù hợp màn hình nhỏ.
- [ ] Food selection.
- [ ] Checkout.
- [ ] Payment result.
- [ ] My bookings.
- [ ] Booking detail/QR ticket.
- [ ] Bottom navigation và back navigation rõ ràng.

## 4. Kiểm tra nghiệp vụ

- [ ] Frontend không thể hiện rằng nó tự quyết định giá; giá/tổng tiền là kết quả backend.
- [ ] Checkout hiện thông tin snapshot mà booking history sẽ sử dụng.
- [ ] Có trạng thái hold hết hạn.
- [ ] Có trạng thái ghế đã bị người khác giữ/đặt.
- [ ] Có retry khi payment failed.
- [ ] Booking PAID mới có QR check-in.
- [ ] Recommendation có trên Home hoặc Movie Detail.
- [ ] Empty/loading/error states có cho các màn hình quan trọng.

## 5. Kiểm tra trước khi gửi giảng viên

- [ ] Đặt section/page rõ ràng: `Web`, `Mobile`, `Design System`.
- [ ] Không còn placeholder hoặc màn hình dang dở trong core flow.
- [ ] Các màn hình có kích thước/frame thống nhất.
- [ ] Mở link bằng cửa sổ ẩn danh thành công.
- [ ] Dán link Web và Mobile vào `README.md` của thư mục review.
- [ ] Export ảnh/PDF dự phòng nếu Stitch gặp lỗi mạng.
