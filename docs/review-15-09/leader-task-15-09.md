# Task của leader cho ngày 15/09

Mục tiêu của leader ngày 15/09 không phải là làm hết phần code. Mục tiêu là giúp giảng viên nhìn thấy nhóm đã có hướng đi rõ ràng: microservice chia theo nghiệp vụ, Stitch có luồng web/mobile, và recommendation có hướng nghiên cứu hợp lý.

## 1. Kết quả cần có trước khi gặp giảng viên

- Bản conceptual chính đã sẵn sàng để mở cho giảng viên xem: `conceptual-review.md`.
- Các chi tiết kỹ thuật cần thiết để trả lời giảng viên đã được giữ trong bản conceptual chính.
- Link Stitch Web đã mở được.
- Link Stitch Mobile đã mở được, dù mobile mới ở mức core flow.
- Checklist Stitch đã được rà một lượt: `stitch-review-checklist.md`.
- Nhóm thống nhất cách giải thích snapshot trong Booking/Order Detail.

## 2. Việc leader cần làm với từng người

Người phụ trách Identity/Catalog:
Xác nhận lại service nào sở hữu Movie, Showtime, Seat, Food, User. Nếu giảng viên hỏi "vì sao Booking không join trực tiếp qua Product/Movie?", người này phải trả lời được rằng mỗi service sở hữu database riêng, service khác chỉ giữ logical ID và snapshot cần thiết.

Người phụ trách Booking/Payment:
Rà lại luồng giữ ghế 3 phút, thanh toán, payment callback, booking paid, sinh QR. Người này phải nắm rõ: Payment không update trực tiếp bảng Booking, mà gửi event hoặc gọi API theo contract đã thống nhất.

Người phụ trách Stitch Web:
Đảm bảo web có các màn hình core: Home, Movie List, Movie Detail, chọn suất chiếu, chọn ghế, chọn đồ ăn, checkout, payment result, booking detail/QR.

Người phụ trách Stitch Mobile:
Làm mobile ở mức đủ để thầy thấy nhóm đã có hướng Flutter: Home, Movie Detail, chọn suất chiếu, chọn ghế, checkout, payment result, My Bookings, QR ticket. Mobile không cần hoàn hảo như web nhưng phải cùng style.

Người phụ trách Recommendation:
Nắm được hướng adaptive hybrid recommendation: content-based cho user ít dữ liệu, collaborative filtering cho user có nhiều interaction, sau đó dùng MMR để đa dạng hóa kết quả. Người này cần nói được đây là giả thuyết để viết paper và sẽ đánh giá bằng Precision@K, Recall@K, NDCG@K, MAP@K.

Leader:
Gom mọi thứ lại, kiểm tra logic giữa conceptual và Stitch, quyết định cái gì đưa thầy xem trước, cái gì để trả lời khi thầy hỏi sâu.

## 3. Checklist leader tự kiểm tra

- [ ] Mở được `README.md` của thư mục review.
- [ ] Mở được `conceptual-review.md`.
- [ ] Mermaid diagram hiển thị được hoặc ít nhất nội dung vẫn đọc được nếu diagram chưa render.
- [ ] Link Stitch Web mở được bằng cửa sổ ẩn danh.
- [ ] Link Stitch Mobile mở được bằng cửa sổ ẩn danh.
- [ ] Web và mobile có cùng flow nghiệp vụ chính.
- [ ] Có màn hình/trạng thái giữ ghế 3 phút.
- [ ] Có payment success, payment failed, booking expired.
- [ ] Có QR ticket sau khi thanh toán thành công.
- [ ] Có khu vực recommendation ở Home hoặc Movie Detail.
- [ ] Nhóm thống nhất: frontend chỉ gửi ID/quantity, backend quyết định giá.
- [ ] Nhóm thống nhất: Booking lưu `productNameSnapshot`, `unitPriceSnapshot`, `movieTitleSnapshot`, `showtimeStartSnapshot`.

## 4. Cách nói với giảng viên trong khoảng 3 phút

Mở đầu:

```text
Thưa thầy, hiện tại project của nhóm em là hệ thống đặt vé xem phim có web, mobile Flutter và recommendation. Với deadline ngày 15, nhóm em tập trung chuẩn bị conceptual design khi chuyển từ monolith sang microservices và bản Stitch cho luồng người dùng chính.
```

Giải thích microservice:

```text
Nhóm em chia hệ thống theo nghiệp vụ chính: Identity quản lý user và role, Catalog quản lý movie/showtime/seat/food/price, Booking quản lý giữ ghế và đặt vé, Payment quản lý giao dịch thanh toán, Recommendation quản lý interaction và gợi ý phim.

Nguyên tắc là mỗi service sở hữu database riêng. Khi cần tham chiếu dữ liệu của service khác, nhóm em dùng logical ID chứ không tạo foreign key trực tiếp giữa database của các service.
```

Giải thích snapshot:

```text
Ví dụ trong BookingFoodItem, nhóm em vẫn lưu productId để biết món đó đến từ Catalog, nhưng đồng thời lưu productNameSnapshot và unitPriceSnapshot. Lý do là khi xem lại lịch sử vé hoặc hóa đơn thì không cần gọi Catalog lại, đồng thời nếu sau này giá/tên sản phẩm thay đổi thì hóa đơn cũ vẫn giữ đúng giá tại thời điểm khách mua.
```

Giải thích flow:

```text
Luồng chính là khách chọn phim, chọn suất chiếu, chọn ghế, hệ thống giữ ghế 3 phút, sau đó khách chọn vé/đồ ăn và thanh toán. Frontend chỉ gửi ID và số lượng; Booking sẽ gọi Catalog/Pricing một lần để lấy dữ liệu và giá có thẩm quyền, sau đó lưu snapshot vào booking.
```

Giải thích recommendation:

```text
Với recommendation, nhóm em đi theo hướng hybrid. User ít tương tác sẽ ưu tiên content-based dựa trên metadata phim. Khi user có nhiều interaction như xem phim, wishlist, booking paid, checked-in hoặc review thì tăng trọng số collaborative filtering. Phần này cũng là hướng để nhóm em viết paper, có công thức scoring và metric đánh giá.
```

Kết thúc:

```text
Phần Stitch hiện tại nhóm em dùng để thể hiện core journey trên web và mobile. Web đã có nền khoảng 60%, mobile đang được dựng theo cùng flow để khi bước sang giai đoạn code Flutter thì nhóm có cùng một logic nghiệp vụ.
```

## 5. Những câu hỏi leader nên chuẩn bị

Nếu thầy hỏi "Tại sao không để Booking gọi Product mỗi lần hiển thị lịch sử?":
Trả lời rằng gọi xuyên service nhiều lần sẽ tăng độ trễ, tăng coupling và dễ tạo N+1 network calls. Snapshot giúp hiển thị lịch sử nhanh hơn và đúng dữ liệu tại thời điểm giao dịch.

Nếu thầy hỏi "Có bị trùng dữ liệu không?":
Trả lời rằng đây là trùng dữ liệu có kiểm soát. Booking không copy toàn bộ Product/Movie, chỉ lưu các field cần cho vé/hóa đơn/lịch sử như tên, giá, thời gian chiếu, phòng chiếu.

Nếu thầy hỏi "Recommendation lấy dữ liệu từ đâu?":
Trả lời rằng Recommendation nhận event/signal từ Catalog, Booking và Interaction API, không đọc trực tiếp database của service khác.

Nếu thầy hỏi "Ngày 15 đã cần code microservice chưa?":
Trả lời rằng deadline ngày 15 là conceptual và Stitch để xác nhận hướng thiết kế. Code migration sẽ nằm trong kế hoạch 2 tháng.

## 6. Việc leader làm sau buổi review

- Ghi lại tất cả góp ý của giảng viên.
- Chốt lại service boundary nếu thầy yêu cầu sửa.
- Cập nhật conceptual nếu có thay đổi.
- Chuyển task từ deadline 15 sang kế hoạch 2 tháng.
- Nhắc từng người bắt đầu theo owner dài hạn đã phân công.
