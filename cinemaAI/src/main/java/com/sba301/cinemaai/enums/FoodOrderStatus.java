package com.sba301.cinemaai.enums;

/**
 * Trạng thái của một đơn bắp nước (FoodOrder).
 * <ul>
 *   <li>{@code PENDING_PAYMENT} — đơn mới tạo, chưa thu tiền (đặt online chờ payment gateway).</li>
 *   <li>{@code PAID} — đã thanh toán; với quầy staff thì chuyển ngay sang PAID khi tạo.</li>
 *   <li>{@code CANCELLED} — đơn bị hủy hoặc hết hạn.</li>
 * </ul>
 */
public enum FoodOrderStatus {
    PENDING_PAYMENT,
    PAID,
    PICKED_UP,
    CANCELLED,
    EXPIRED
}
