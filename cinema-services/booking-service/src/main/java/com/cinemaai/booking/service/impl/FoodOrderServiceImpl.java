package com.cinemaai.booking.service.impl;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.client.dto.CatalogFoodQuoteDto;
import com.cinemaai.booking.dto.request.BookingFoodRequest;
import com.cinemaai.booking.dto.request.FoodOrderRequest;
import com.cinemaai.booking.dto.response.FoodOrderItemResponse;
import com.cinemaai.booking.dto.response.FoodOrderResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.FoodOrder;
import com.cinemaai.booking.entity.FoodOrderItem;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.FoodOrderRepository;
import com.cinemaai.booking.service.FoodOrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodOrderServiceImpl implements FoodOrderService {

    private static final long PAYMENT_WINDOW_MINUTES = 15;

    private final FoodOrderRepository foodOrderRepository;
    private final BookingRepository bookingRepository;
    private final CatalogClient catalogClient;
    private final PaymentClient paymentClient;

    private static final Map<Long, String> FALLBACK_ITEM_NAMES = Map.ofEntries(
            Map.entry(1L, "Bắp Rang Bơ Truyền Thống 64oz"),
            Map.entry(2L, "Bắp Rang Phô Mai 64oz"),
            Map.entry(3L, "Bắp Rang Caramel 64oz"),
            Map.entry(4L, "Bắp Rang Socola 64oz"),
            Map.entry(5L, "Coca-Cola 32oz"),
            Map.entry(6L, "Sprite 32oz"),
            Map.entry(7L, "Fanta Cam 32oz"),
            Map.entry(8L, "Trà Đào Hạt Chia 32oz"),
            Map.entry(9L, "Nước Khoáng Dasani 500ml"),
            Map.entry(10L, "Xúc Xích Phô Mai Nướng"),
            Map.entry(11L, "Khoai Tây Lắc Phô Mai")
    );

    private static final Map<Long, BigDecimal> FALLBACK_ITEM_PRICES = Map.ofEntries(
            Map.entry(1L, BigDecimal.valueOf(45000)),
            Map.entry(2L, BigDecimal.valueOf(55000)),
            Map.entry(3L, BigDecimal.valueOf(55000)),
            Map.entry(4L, BigDecimal.valueOf(55000)),
            Map.entry(5L, BigDecimal.valueOf(35000)),
            Map.entry(6L, BigDecimal.valueOf(35000)),
            Map.entry(7L, BigDecimal.valueOf(35000)),
            Map.entry(8L, BigDecimal.valueOf(40000)),
            Map.entry(9L, BigDecimal.valueOf(20000)),
            Map.entry(10L, BigDecimal.valueOf(45000)),
            Map.entry(11L, BigDecimal.valueOf(45000))
    );

    private static final Map<Long, String> FALLBACK_COMBO_NAMES = Map.ofEntries(
            Map.entry(1L, "Combo Solo Movie"),
            Map.entry(2L, "Combo Couple Sweet"),
            Map.entry(3L, "Combo Party Friends"),
            Map.entry(4L, "Combo VIP Deluxe"),
            Map.entry(5L, "Combo Gia Đình (Family Pack)")
    );

    private static final Map<Long, BigDecimal> FALLBACK_COMBO_PRICES = Map.ofEntries(
            Map.entry(1L, BigDecimal.valueOf(79000)),
            Map.entry(2L, BigDecimal.valueOf(109000)),
            Map.entry(3L, BigDecimal.valueOf(189000)),
            Map.entry(4L, BigDecimal.valueOf(169000)),
            Map.entry(5L, BigDecimal.valueOf(229000))
    );

    @Override
    @Transactional
    public FoodOrderResponse createStandalone(Long userId, FoodOrderRequest request) {
        return toResponse(createOrder(null, userId, request));
    }

    @Override
    @Transactional
    public FoodOrderResponse createForBooking(Long userId, Long bookingId, FoodOrderRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin vé #" + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new NotFoundException("Đơn đặt vé không thuộc về người dùng hiện tại");
        }
        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.USED) {
            throw new BadRequestException("Chỉ có thể đặt thêm bắp nước cho vé đã thanh toán");
        }

        return toResponse(createOrder(booking, userId, request));
    }

    @Override
    @Transactional
    public List<FoodOrderResponse> listMine(Long userId) {
        List<FoodOrder> orders = foodOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();

        for (FoodOrder order : orders) {
            if ("PENDING_PAYMENT".equalsIgnoreCase(order.getStatus())) {
                LocalDateTime expiresAt = getExpiresAt(order);
                if (now.isAfter(expiresAt)) {
                    order.setStatus("EXPIRED");
                }
            }
        }
        return orders.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodOrderResponse> listByBooking(Long userId, Long bookingId) {
        return foodOrderRepository.findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream()
                .filter(o -> o.getUserId().equals(userId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public FoodOrderResponse cancel(Long userId, Long foodOrderId) {
        FoodOrder order = foodOrderRepository.findById(foodOrderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn bắp nước #" + foodOrderId));

        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thao tác trên đơn bắp nước này");
        }

        if ("PENDING_PAYMENT".equalsIgnoreCase(order.getStatus())) {
            LocalDateTime expiresAt = getExpiresAt(order);
            if (LocalDateTime.now().isAfter(expiresAt)) {
                order.setStatus("EXPIRED");
                return toResponse(order);
            }
            order.setStatus("CANCELLED");
            order.setCancelledAt(LocalDateTime.now());
            return toResponse(order);
        }

        throw new BadRequestException("Chỉ có thể hủy đơn bắp nước đang chờ thanh toán");
    }

    @Override
    @Transactional(readOnly = true)
    public FoodOrderResponse getById(Long foodOrderId) {
        FoodOrder order = foodOrderRepository.findById(foodOrderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn bắp nước #" + foodOrderId));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodOrderResponse getByIdAndUser(Long userId, Long foodOrderId) {
        FoodOrder order = foodOrderRepository.findByIdAndUserId(foodOrderId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn bắp nước #" + foodOrderId));
        return toResponse(order);
    }

    @Override
    @Transactional
    public FoodOrderResponse markPaid(Long foodOrderId, String transactionId) {
        FoodOrder order = foodOrderRepository.findById(foodOrderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn bắp nước #" + foodOrderId));

        order.setStatus("PAID");
        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
        FoodOrder saved = foodOrderRepository.save(order);
        log.info("Successfully marked FoodOrder {} as PAID (transactionId: {})", saved.getFoodOrderCode(), transactionId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FoodOrderResponse syncPaymentStatus(Long userId, Long foodOrderId) {
        FoodOrder order = foodOrderRepository.findById(foodOrderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn bắp nước #" + foodOrderId));

        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thao tác trên đơn này");
        }

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            return toResponse(order);
        }

        try {
            boolean isPaid = paymentClient.checkFoodOrderPaymentSuccess(foodOrderId);
            if (isPaid) {
                order.setStatus("PAID");
                if (order.getPaidAt() == null) {
                    order.setPaidAt(LocalDateTime.now());
                }
                foodOrderRepository.save(order);
                log.info("Synced FoodOrder {} status to PAID via PaymentClient", order.getFoodOrderCode());
            }
        } catch (Exception ex) {
            log.warn("Failed to check payment status from Payment Service for food order {}: {}", foodOrderId, ex.getMessage());
        }

        return toResponse(order);
    }

    private FoodOrder createOrder(Booking booking, Long userId, FoodOrderRequest request) {
        if (request == null || request.foods() == null || request.foods().isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một món bắp nước");
        }

        String orderCode = "FO-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 900 + 100);

        FoodOrder order = FoodOrder.builder()
                .foodOrderCode(orderCode)
                .userId(userId)
                .bookingId(booking != null ? booking.getId() : null)
                .cinemaId(request != null ? request.cinemaId() : null)
                .cinemaName(request != null && request.cinemaName() != null ? request.cinemaName() : "CinemaAI Dragon City")
                .cinemaAddress(request != null && request.cinemaAddress() != null ? request.cinemaAddress() : "Tầng 5, Vincom Plaza Ngô Quyền, 910A Ngô Quyền, Q. Sơn Trà, Đà Nẵng")
                .status("PENDING_PAYMENT")
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        // 1. Attempt quote from Catalog Service
        List<CatalogFoodQuoteDto.Request.Item> quoteItems = new ArrayList<>();
        for (BookingFoodRequest item : request.foods()) {
            quoteItems.add(new CatalogFoodQuoteDto.Request.Item(
                    item.foodItemId(),
                    item.foodComboId(),
                    item.productId(),
                    item.isCombo(),
                    item.quantity()
            ));
        }

        CatalogFoodQuoteDto.Response quoteResponse = catalogClient.getFoodQuote(new CatalogFoodQuoteDto.Request(quoteItems));

        BigDecimal total = BigDecimal.ZERO;

        if (quoteResponse != null && quoteResponse.items() != null && !quoteResponse.items().isEmpty()) {
            for (CatalogFoodQuoteDto.Response.ItemSnapshot snap : quoteResponse.items()) {
                FoodOrderItem orderItem = FoodOrderItem.builder()
                        .foodOrder(order)
                        .productId(snap.productId())
                        .isCombo(snap.isCombo())
                        .productNameSnapshot(snap.productName())
                        .quantity(snap.quantity())
                        .unitPrice(snap.unitPrice())
                        .lineTotal(snap.lineTotal())
                        .build();
                order.getItems().add(orderItem);
                total = total.add(snap.lineTotal());
            }
        } else {
            // Fallback resolution using predefined catalog
            for (BookingFoodRequest foodReq : request.foods()) {
                Long productId = foodReq.productId();
                boolean isCombo = Boolean.TRUE.equals(foodReq.isCombo());

                if (productId == null) {
                    if (foodReq.foodComboId() != null) {
                        productId = foodReq.foodComboId();
                        isCombo = true;
                    } else if (foodReq.foodItemId() != null) {
                        productId = foodReq.foodItemId();
                        isCombo = false;
                    }
                }

                if (productId == null) continue;
                int qty = foodReq.quantity() > 0 ? foodReq.quantity() : 1;

                String name = isCombo
                        ? FALLBACK_COMBO_NAMES.getOrDefault(productId, "Combo #" + productId)
                        : FALLBACK_ITEM_NAMES.getOrDefault(productId, "Món bắp nước #" + productId);

                BigDecimal unitPrice = isCombo
                        ? FALLBACK_COMBO_PRICES.getOrDefault(productId, BigDecimal.valueOf(109000))
                        : FALLBACK_ITEM_PRICES.getOrDefault(productId, BigDecimal.valueOf(45000));

                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));

                FoodOrderItem orderItem = FoodOrderItem.builder()
                        .foodOrder(order)
                        .productId(productId)
                        .isCombo(isCombo)
                        .productNameSnapshot(name)
                        .quantity(qty)
                        .unitPrice(unitPrice)
                        .lineTotal(lineTotal)
                        .build();

                order.getItems().add(orderItem);
                total = total.add(lineTotal);
            }
        }

        order.setSubtotal(total);
        order.setTotalAmount(total);

        return foodOrderRepository.save(order);
    }

    private LocalDateTime getExpiresAt(FoodOrder order) {
        LocalDateTime base = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
        return base.plusMinutes(PAYMENT_WINDOW_MINUTES);
    }

    private FoodOrderResponse toResponse(FoodOrder order) {
        String bookingCode = null;
        if (order.getBookingId() != null) {
            bookingCode = bookingRepository.findById(order.getBookingId())
                    .map(Booking::getBookingCode)
                    .orElse(null);
        }

        List<FoodOrderItemResponse> items = new ArrayList<>();
        if (order.getItems() != null) {
            for (FoodOrderItem item : order.getItems()) {
                Long foodItemId = item.isCombo() ? null : item.getProductId();
                Long foodComboId = item.isCombo() ? item.getProductId() : null;
                items.add(new FoodOrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        foodItemId,
                        foodComboId,
                        item.isCombo(),
                        item.getProductNameSnapshot(),
                        item.getProductNameSnapshot(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ));
            }
        }

        LocalDateTime expiresAt = getExpiresAt(order);

        String qrCode = "PAID".equalsIgnoreCase(order.getStatus())
                ? ("FOOD:" + order.getFoodOrderCode() + ":" + order.getId())
                : null;

        return new FoodOrderResponse(
                order.getId(),
                order.getFoodOrderCode(),
                order.getFoodOrderCode(),
                order.getBookingId(),
                bookingCode,
                order.getStatus(),
                order.getSubtotal(),
                order.getTotalAmount(),
                order.getPaidAt(),
                expiresAt,
                order.getCancelledAt(),
                order.getCreatedAt(),
                qrCode,
                order.getCinemaId(),
                order.getCinemaName() != null ? order.getCinemaName() : "CinemaAI Dragon City",
                order.getCinemaAddress() != null ? order.getCinemaAddress() : "Tầng 5, Vincom Plaza Ngô Quyền, 910A Ngô Quyền, Q. Sơn Trà, Đà Nẵng",
                items
        );
    }
}