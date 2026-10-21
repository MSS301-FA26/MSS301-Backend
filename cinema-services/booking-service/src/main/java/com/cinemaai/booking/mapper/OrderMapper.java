package com.cinemaai.booking.mapper;

import com.cinemaai.booking.dto.response.OrderItemResponse;
import com.cinemaai.booking.dto.response.OrderResponse;
import com.cinemaai.booking.entity.Order;
import com.cinemaai.booking.entity.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "foodItemId", expression = "java(item.isCombo() ? null : item.getProductId())")
    @Mapping(target = "foodComboId", expression = "java(item.isCombo() ? item.getProductId() : null)")
    @Mapping(target = "isCombo", expression = "java(item.isCombo())")
    @Mapping(target = "name", source = "item.productNameSnapshot")
    @Mapping(target = "productName", source = "item.productNameSnapshot")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "orderCode", source = "order.foodOrderCode")
    @Mapping(target = "foodOrderCode", source = "order.foodOrderCode")
    @Mapping(target = "bookingId", source = "order.bookingId")
    @Mapping(target = "bookingCode", source = "bookingCode")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "subtotal", source = "order.subtotal")
    @Mapping(target = "totalAmount", source = "order.totalAmount")
    @Mapping(target = "paidAt", source = "order.paidAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "cancelledAt", source = "order.cancelledAt")
    @Mapping(target = "createdAt", source = "order.createdAt")
    @Mapping(target = "qrCode", source = "qrCode")
    @Mapping(target = "cinemaId", source = "order.cinemaId")
    @Mapping(target = "cinemaName", expression = "java(order.getCinemaName() != null ? order.getCinemaName() : \"CinemaAI Dragon City\")")
    @Mapping(target = "cinemaAddress", expression = "java(order.getCinemaAddress() != null ? order.getCinemaAddress() : \"Tầng 5, Vincom Plaza Ngô Quyền, 910A Ngô Quyền, Q. Sơn Trà, Đà Nẵng\")")
    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(
            Order order,
            String bookingCode,
            LocalDateTime expiresAt,
            String qrCode,
            List<OrderItemResponse> items
    );
}
