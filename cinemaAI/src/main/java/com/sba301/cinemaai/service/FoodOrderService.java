package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.booking.FoodOrderRequest;
import com.sba301.cinemaai.dto.response.booking.FoodOrderResponse;
import java.util.List;

public interface FoodOrderService {

    /** Khách hàng mua bắp nước độc lập, không cần có vé xem phim. */
    FoodOrderResponse createStandalone(String email, FoodOrderRequest request);

    /** Danh sách đơn bắp nước của khách, độc lập với danh sách vé xem phim. */
    List<FoodOrderResponse> listMine(String email);

    /** Hủy chủ động một đơn chưa thanh toán. */
    FoodOrderResponse cancel(String email, Long foodOrderId);

    /** Chuyển các đơn quá hạn sang EXPIRED; được gọi bởi scheduler và trước các thao tác đọc/ghi. */
    int expirePendingOrders();

    /**
     * Khách hàng đặt thêm bắp nước online cho booking của mình.
     * Booking phải ở trạng thái PAID hoặc USED và suất chiếu chưa kết thúc.
     */
    FoodOrderResponse create(String email, Long bookingId, FoodOrderRequest request);

    /** Trả về tất cả đơn bắp nước của một booking — chỉ chủ booking mới được xem. */
    List<FoodOrderResponse> listByBooking(String email, Long bookingId);

    /**
     * Staff quầy thu ngân tạo đơn và thu tiền mặt ngay tại quầy.
     * {@code bookingOrTicketCode} có thể là booking code, ticket code, hoặc chuỗi QR ({@code CINEAI:…}).
     * Payment được ghi với provider CASH để tách doanh thu quầy khỏi giao dịch online trong báo cáo.
     */
    FoodOrderResponse createStaffOrder(String bookingOrTicketCode, FoodOrderRequest request);

    /** Staff tra cứu đơn bắp nước bằng mã đơn hoặc QR nhận món. */
    FoodOrderResponse lookupForPickup(String code);

    /** Staff xác nhận giao món một lần; đơn PAID chuyển sang PICKED_UP. */
    FoodOrderResponse markPickedUp(String code);
}
