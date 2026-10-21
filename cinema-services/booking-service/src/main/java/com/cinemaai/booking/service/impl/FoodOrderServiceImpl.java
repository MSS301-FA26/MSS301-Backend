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
import com.cinemaai.booking.mapper.FoodOrderMapper;
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
    private final FoodOrderMapper foodOrderMapper;

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
        validateFoodSelections(request.foods());

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

        if (quoteResponse == null || quoteResponse.items() == null || quoteResponse.items().size() != request.foods().size()) {
            throw new BadRequestException("Unable to verify food availability and pricing");
        }
        Set<String> requestedProducts = request.foods().stream()
                .map(food -> {
                    Long id = food.productId() != null ? food.productId()
                            : food.foodComboId() != null ? food.foodComboId() : food.foodItemId();
                    boolean combo = food.foodComboId() != null || Boolean.TRUE.equals(food.isCombo());
                    return combo + ":" + id + ":" + food.quantity();
                })
                .collect(java.util.stream.Collectors.toSet());
        Set<String> quotedProducts = new HashSet<>();
        for (CatalogFoodQuoteDto.Response.ItemSnapshot snap : quoteResponse.items()) {
                if (snap.productId() == null || snap.productId() <= 0 || snap.quantity() <= 0
                        || snap.unitPrice() == null || snap.unitPrice().signum() < 0
                        || snap.lineTotal() == null || snap.lineTotal().signum() < 0
                        || snap.unitPrice().multiply(BigDecimal.valueOf(snap.quantity())).compareTo(snap.lineTotal()) != 0) {
                    throw new BadRequestException("Catalog returned an invalid food quote");
                }
                if (!quotedProducts.add(snap.isCombo() + ":" + snap.productId() + ":" + snap.quantity()))
                    throw new BadRequestException("Catalog returned duplicate food items");
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
        if (!quotedProducts.equals(requestedProducts))
            throw new BadRequestException("Catalog food quote does not match requested products");
        if (quoteResponse.totalAmount() == null || quoteResponse.totalAmount().signum() < 0
                || total.compareTo(quoteResponse.totalAmount()) != 0)
            throw new BadRequestException("Catalog food total is inconsistent");

        order.setSubtotal(total);
        order.setTotalAmount(total);

        return foodOrderRepository.save(order);
    }

    private void validateFoodSelections(List<BookingFoodRequest> foods) {
        Set<String> keys = new HashSet<>();
        for (BookingFoodRequest food : foods) {
            if (food == null || food.quantity() <= 0) throw new BadRequestException("Food quantity must be greater than zero");
            Long productId = food.productId();
            Long itemId = food.foodItemId();
            Long comboId = food.foodComboId();
            if (itemId != null && comboId != null) throw new BadRequestException("Food item and combo identifiers conflict");
            if (productId == null && itemId == null && comboId == null) throw new BadRequestException("Food product identifier is required");
            if (productId != null && productId <= 0 || itemId != null && itemId <= 0 || comboId != null && comboId <= 0)
                throw new BadRequestException("Food product ID must be positive");
            if (productId != null && itemId != null && (!productId.equals(itemId) || Boolean.TRUE.equals(food.isCombo())))
                throw new BadRequestException("Food product identifiers conflict");
            if (productId != null && comboId != null && (!productId.equals(comboId) || !Boolean.TRUE.equals(food.isCombo())))
                throw new BadRequestException("Food product identifiers conflict");
            if (productId != null && itemId == null && comboId == null && food.isCombo() == null)
                throw new BadRequestException("Food product type is required");
            Long id = productId != null ? productId : comboId != null ? comboId : itemId;
            boolean combo = comboId != null || Boolean.TRUE.equals(food.isCombo());
            if (!keys.add(combo + ":" + id)) throw new BadRequestException("Duplicate food selection");
        }
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

        List<FoodOrderItemResponse> items = order.getItems() != null
                ? foodOrderMapper.toItemResponseList(order.getItems())
                : Collections.emptyList();

        LocalDateTime expiresAt = getExpiresAt(order);

        String qrCode = "PAID".equalsIgnoreCase(order.getStatus())
                ? ("FOOD:" + order.getFoodOrderCode() + ":" + order.getId())
                : null;

        return foodOrderMapper.toResponse(order, bookingCode, expiresAt, qrCode, items);
    }
}
