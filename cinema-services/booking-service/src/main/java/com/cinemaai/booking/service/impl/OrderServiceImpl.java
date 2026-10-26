package com.cinemaai.booking.service.impl;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.client.dto.CatalogProductQuoteDto;
import com.cinemaai.booking.dto.request.BookingProductRequest;
import com.cinemaai.booking.dto.request.OrderRequest;
import com.cinemaai.booking.dto.response.OrderItemResponse;
import com.cinemaai.booking.dto.response.OrderResponse;
import com.cinemaai.booking.dto.response.PageResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.Order;
import com.cinemaai.booking.entity.OrderItem;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.OrderRepository;
import com.cinemaai.booking.service.OrderService;
import com.cinemaai.booking.mapper.OrderMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final long PAYMENT_WINDOW_MINUTES = 15;

    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final CatalogClient catalogClient;
    private final PaymentClient paymentClient;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse createStandalone(Long userId, OrderRequest request) {
        return toResponse(createOrder(null, userId, request));
    }

    @Override
    @Transactional
    public OrderResponse createForBooking(Long userId, Long bookingId, OrderRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y thÃ´ng tin vÃ© #" + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new NotFoundException("ÄÆ¡n Ä‘áº·t vÃ© khÃ´ng thuá»™c vá» ngÆ°á»i dÃ¹ng hiá»‡n táº¡i");
        }
        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.USED) {
            throw new BadRequestException("Chá»‰ cÃ³ thá»ƒ Ä‘áº·t thÃªm báº¯p nÆ°á»›c cho vÃ© Ä‘Ã£ thanh toÃ¡n");
        }

        return toResponse(createOrder(booking, userId, request));
    }

    @Override
    @Transactional
    public List<OrderResponse> listMine(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();

        for (Order order : orders) {
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
    public PageResponse<OrderResponse> listAll(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        PageRequest stablePage = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        return PageResponse.from(orderRepository.findAll(stablePage).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listByBooking(Long userId, Long bookingId) {
        return orderRepository.findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream()
                .filter(o -> o.getUserId().equals(userId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long userId, Long foodOrderId) {
        Order order = orderRepository.findByIdAndUserId(foodOrderId, userId)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n báº¯p nÆ°á»›c #" + foodOrderId));

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

        throw new BadRequestException("Chá»‰ cÃ³ thá»ƒ há»§y Ä‘Æ¡n báº¯p nÆ°á»›c Ä‘ang chá» thanh toÃ¡n");
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long foodOrderId) {
        Order order = orderRepository.findById(foodOrderId)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n báº¯p nÆ°á»›c #" + foodOrderId));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByIdAndUser(Long userId, Long foodOrderId) {
        Order order = orderRepository.findByIdAndUserId(foodOrderId, userId)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n báº¯p nÆ°á»›c #" + foodOrderId));
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse markPaid(Long foodOrderId, String transactionId) {
        Order order = orderRepository.findById(foodOrderId)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n báº¯p nÆ°á»›c #" + foodOrderId));

        order.setStatus("PAID");
        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
        Order saved = orderRepository.save(order);
        log.info("Successfully marked Order {} as PAID (transactionId: {})", saved.getFoodOrderCode(), transactionId);
        if (saved.getUserId() != null && saved.getTotalAmount() != null && saved.getTotalAmount().signum() > 0) {
            paymentClient.awardFoodOrderLoyaltyPoints(
                    saved.getUserId(), saved.getId(), saved.getFoodOrderCode(),
                    saved.getTotalAmount(), saved.getCinemaId());
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse syncPaymentStatus(Long userId, Long foodOrderId) {
        Order order = orderRepository.findByIdAndUserId(foodOrderId, userId)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n báº¯p nÆ°á»›c #" + foodOrderId));

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
                orderRepository.save(order);
                log.info("Synced Order {} status to PAID via PaymentClient", order.getFoodOrderCode());
                if (order.getUserId() != null && order.getTotalAmount() != null && order.getTotalAmount().signum() > 0) {
                    paymentClient.awardFoodOrderLoyaltyPoints(
                            order.getUserId(), order.getId(), order.getFoodOrderCode(),
                            order.getTotalAmount(), order.getCinemaId());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to check payment status from Payment Service for food order {}: {}", foodOrderId, ex.getMessage());
        }

        return toResponse(order);
    }

    private Order createOrder(Booking booking, Long userId, OrderRequest request) {
        if (request == null || request.foods() == null || request.foods().isEmpty()) {
            throw new BadRequestException("Vui lÃ²ng chá»n Ã­t nháº¥t má»™t mÃ³n báº¯p nÆ°á»›c");
        }
        validateFoodSelections(request.foods());

        String orderCode = "FO-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 900 + 100);

        Order order = Order.builder()
                .foodOrderCode(orderCode)
                .userId(userId)
                .bookingId(booking != null ? booking.getId() : null)
                .cinemaId(request != null ? request.cinemaId() : null)
                .cinemaName(request != null && request.cinemaName() != null ? request.cinemaName() : "CinemaAI Dragon City")
                .cinemaAddress(request != null && request.cinemaAddress() != null ? request.cinemaAddress() : "Táº§ng 5, Vincom Plaza NgÃ´ Quyá»n, 910A NgÃ´ Quyá»n, Q. SÆ¡n TrÃ , ÄÃ  Náºµng")
                .status("PENDING_PAYMENT")
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        // 1. Attempt quote from Catalog Service
        List<CatalogProductQuoteDto.Request.Item> quoteItems = new ArrayList<>();
        for (BookingProductRequest item : request.foods()) {
            quoteItems.add(new CatalogProductQuoteDto.Request.Item(
                    item.foodItemId(),
                    item.foodComboId(),
                    item.productId(),
                    item.isCombo(),
                    item.quantity()
            ));
        }

        CatalogProductQuoteDto.Response quoteResponse = catalogClient.getFoodQuote(new CatalogProductQuoteDto.Request(quoteItems));

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
        for (CatalogProductQuoteDto.Response.ItemSnapshot snap : quoteResponse.items()) {
                if (snap.productId() == null || snap.productId() <= 0 || snap.quantity() <= 0
                        || snap.unitPrice() == null || snap.unitPrice().signum() < 0
                        || snap.lineTotal() == null || snap.lineTotal().signum() < 0
                        || snap.unitPrice().multiply(BigDecimal.valueOf(snap.quantity())).compareTo(snap.lineTotal()) != 0) {
                    throw new BadRequestException("Catalog returned an invalid food quote");
                }
                if (!quotedProducts.add(snap.isCombo() + ":" + snap.productId() + ":" + snap.quantity()))
                    throw new BadRequestException("Catalog returned duplicate food items");
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
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

        return orderRepository.save(order);
    }

    private void validateFoodSelections(List<BookingProductRequest> foods) {
        Set<String> keys = new HashSet<>();
        for (BookingProductRequest food : foods) {
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

    private LocalDateTime getExpiresAt(Order order) {
        LocalDateTime base = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
        return base.plusMinutes(PAYMENT_WINDOW_MINUTES);
    }

    private OrderResponse toResponse(Order order) {
        String bookingCode = null;
        if (order.getBookingId() != null) {
            bookingCode = bookingRepository.findById(order.getBookingId())
                    .map(Booking::getBookingCode)
                    .orElse(null);
        }

        List<OrderItemResponse> items = order.getItems() != null
                ? orderMapper.toItemResponseList(order.getItems())
                : Collections.emptyList();

        LocalDateTime expiresAt = getExpiresAt(order);

        String qrCode = "PAID".equalsIgnoreCase(order.getStatus())
                ? ("FOOD:" + order.getFoodOrderCode() + ":" + order.getId())
                : null;

        return orderMapper.toResponse(order, bookingCode, expiresAt, qrCode, items);
    }
}

