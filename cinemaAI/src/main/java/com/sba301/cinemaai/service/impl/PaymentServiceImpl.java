package com.sba301.cinemaai.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.cinemaai.dto.response.payment.PaymentResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.entity.FoodOrder;
import com.sba301.cinemaai.entity.Payment;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.FoodOrderStatus;
import com.sba301.cinemaai.enums.PaymentProvider;
import com.sba301.cinemaai.util.PaymentAccountSupport;
import com.sba301.cinemaai.enums.PaymentStatus;
import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.FoodOrderRepository;
import com.sba301.cinemaai.repository.PaymentRepository;
import com.sba301.cinemaai.service.LoyaltyPointService;
import com.sba301.cinemaai.service.NotificationService;
import com.sba301.cinemaai.service.PaymentService;
import com.sba301.cinemaai.service.QrTicketService;
import com.sba301.cinemaai.service.VNPayService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final VNPayService vnpayService;
    private final QrTicketService qrTicketService;
    private final LoyaltyPointService loyaltyPointService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse createVnpayPayment(String email, Long bookingId, String clientIp) {
        Booking booking = findBooking(bookingId);
        validateBookingOwner(booking, email);

        if (booking.getStatus() != BookingStatus.HOLDING && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Only HOLDING or PENDING_PAYMENT bookings can be paid");
        }

        // Retry thanh toán là bình thường (user thoát VNPay giữa chừng rồi "Tiếp tục thanh toán"):
        // hủy các payment VÉ đang PENDING cũ rồi tạo phiên mới, thay vì ném 409.
        supersedePendingTicketPayments(booking);

        Payment payment = paymentRepository.save(new Payment(booking, PaymentProvider.VNPAY, booking.getTotalAmount()));
        if (booking.getStatus() == BookingStatus.HOLDING) {
            markPendingPayment(booking);
        }

        String txnRef = payment.getId() + "-" + booking.getBookingCode();
        String orderInfo = "Thanh toan ve xem phim " + booking.getBookingCode();
        log.info(
                "Creating VNPAY payment for booking {}: subtotal={}, discount={}, total={}, redeemedPoints={}",
                booking.getBookingCode(),
                booking.getSubtotal(),
                booking.getDiscountAmount(),
                booking.getTotalAmount(),
                booking.getLoyaltyPointsRedeemed()
        );
        String paymentUrl = vnpayService.buildPaymentUrl(txnRef, booking.getTotalAmount(), orderInfo, clientIp);

        return toResponse(payment, paymentUrl);
    }

    @Transactional
    public String handleVnpayReturn(Map<String, String> params) {
        if (!vnpayService.verifySignature(params)) {
            log.warn("VNPAY return: invalid signature, params={}", params);
            return "INVALID_SIGNATURE";
        }

        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        String transactionNo = params.get("vnp_TransactionNo");

        Payment payment = findPaymentByTxnRef(txnRef);
        if (payment == null) return "PAYMENT_NOT_FOUND";

        if ("00".equals(responseCode)) {
            confirmPayment(payment, transactionNo, params);
            return "SUCCESS";
        } else {
            markPaymentFailed(payment, toJson(params));
            log.info("VNPAY return: payment {} failed, code={}", payment.getId(), responseCode);
            return "FAILED";
        }
    }

    @Transactional
    public String handleVnpayIpn(Map<String, String> params) {
        if (!vnpayService.verifySignature(params)) {
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}";
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionNo = params.get("vnp_TransactionNo");

        Payment payment = findPaymentByTxnRef(txnRef);
        if (payment == null) {
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
        }

        BigDecimal vnpAmount = new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100));
        if (vnpAmount.compareTo(payment.getAmount()) != 0) {
            return "{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}";
        }

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            confirmPayment(payment, transactionNo, params);
        } else {
            markPaymentFailed(payment, toJson(params));
        }

        return "{\"RspCode\":\"00\",\"Message\":\"Confirm success\"}";
    }

    @Transactional
    public PaymentResponse mockPayment(String email, Long bookingId) {
        Booking booking = findBooking(bookingId);
        validateBookingOwner(booking, email);
        if (booking.getStatus() != BookingStatus.HOLDING && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Only HOLDING or PENDING_PAYMENT bookings can be paid");
        }
        if (booking.getStatus() == BookingStatus.HOLDING) {
            markPendingPayment(booking);
        }
        Payment payment = paymentRepository.save(new Payment(booking, PaymentProvider.MOCK, booking.getTotalAmount()));
        confirmPayment(payment, "MOCK-" + System.currentTimeMillis(), Map.of("provider", "MOCK"));
        return toResponse(payment, null);
    }

    @Transactional
    public PaymentResponse createVnpayFoodOrderPayment(String email, Long foodOrderId, String clientIp) {
        FoodOrder foodOrder = findFoodOrder(foodOrderId);
        validateFoodOrderOwner(foodOrder, email);
        requirePayableFoodOrder(foodOrder);

        // Retry: hủy payment PENDING cũ của đúng food order này rồi tạo phiên mới
        paymentRepository.findByFoodOrder(foodOrder)
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .forEach(p -> markPaymentFailed(p, "SUPERSEDED_BY_RETRY"));

        Payment payment = new Payment(foodOrder.getBooking(), PaymentProvider.VNPAY, foodOrder.getTotalAmount());
        payment.setFoodOrder(foodOrder);
        payment = paymentRepository.save(payment);

        String txnRef = payment.getId() + "-" + foodOrder.getOrderCode();
        String orderInfo = "Thanh toan bap nuoc " + foodOrder.getOrderCode();
        String paymentUrl = vnpayService.buildPaymentUrl(
                txnRef, foodOrder.getTotalAmount(), orderInfo, clientIp, foodOrder.getExpiresAt());
        return toResponse(payment, paymentUrl);
    }

    @Transactional
    public PaymentResponse mockFoodOrderPayment(String email, Long foodOrderId) {
        FoodOrder foodOrder = findFoodOrder(foodOrderId);
        validateFoodOrderOwner(foodOrder, email);
        requirePayableFoodOrder(foodOrder);

        Payment payment = new Payment(foodOrder.getBooking(), PaymentProvider.MOCK, foodOrder.getTotalAmount());
        payment.setFoodOrder(foodOrder);
        payment = paymentRepository.save(payment);
        confirmPayment(payment, "MOCK-" + System.currentTimeMillis(), Map.of("provider", "MOCK"));
        return toResponse(payment, null);
    }

    private void requirePayableFoodOrder(FoodOrder foodOrder) {
        if (foodOrder.getStatus() != FoodOrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Only pending food orders can be paid");
        }
        if (foodOrder.getExpiresAt() == null || !LocalDateTime.now().isBefore(foodOrder.getExpiresAt())) {
            throw new BadRequestException("Food order payment window has expired");
        }
    }

    private FoodOrder findFoodOrder(Long foodOrderId) {
        return foodOrderRepository.findById(foodOrderId)
                .orElseThrow(() -> new NotFoundException("Food order not found"));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByBooking(String email, Long bookingId) {
        Booking booking = findBooking(bookingId);
        validateBookingOwner(booking, email);
        return paymentRepository.findFirstByBookingOrderByIdDesc(booking)
                .map(p -> toResponse(p, null))
                .orElseThrow(() -> new NotFoundException("No payment found for this booking"));
    }

    private void confirmPayment(Payment payment, String transactionNo, Map<String, String> params) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) return;

        if (payment.getFoodOrder() != null) {
            FoodOrder foodOrder = payment.getFoodOrder();
            if (foodOrder.getStatus() != FoodOrderStatus.PENDING_PAYMENT
                    || foodOrder.getExpiresAt() == null
                    || !LocalDateTime.now().isBefore(foodOrder.getExpiresAt())) {
                markPaymentFailed(payment, "FOOD_ORDER_EXPIRED: " + toJson(params));
                if (foodOrder.getStatus() == FoodOrderStatus.PENDING_PAYMENT) {
                    foodOrder.setStatus(FoodOrderStatus.EXPIRED);
                }
                return;
            }
        } else {
            Booking booking = payment.getBooking();
            LocalDateTime holdExpiry = booking.getHoldExpiresAt();
            if (holdExpiry != null && LocalDateTime.now().isAfter(holdExpiry)) {
                // Hold đã hết hạn — từ chối xác nhận, không được bán ghế
                log.warn(
                    "Payment {} for booking {} REJECTED: hold expired at {} (now={}). Seats NOT confirmed.",
                    payment.getId(), booking.getBookingCode(), holdExpiry, LocalDateTime.now()
                );
                markPaymentFailed(payment, "HOLD_EXPIRED: " + toJson(params));
                return;
            }
        }

        markPaymentSuccess(payment, transactionNo, toJson(params), params);

        if (payment.getFoodOrder() != null) {
            FoodOrder foodOrder = payment.getFoodOrder();
            foodOrder.setStatus(FoodOrderStatus.PAID);
            foodOrder.setPaidAt(LocalDateTime.now());
            var customer = foodOrder.getCustomer() != null
                    ? foodOrder.getCustomer()
                    : foodOrder.getBooking().getUser();
            loyaltyPointService.addPointsFromFoodOrder(customer, foodOrder);
            log.info("Payment {} confirmed for food order {}", payment.getId(), foodOrder.getOrderCode());
            return;
        }

        Booking booking = payment.getBooking();
        bookingSeatRepository.findByBooking(booking)
                .forEach(seat -> changeBookingSeatStatus(seat, SeatRuntimeStatus.BOOKED));
        markPaid(booking, qrTicketService.generate(booking));
        generateSeatTickets(booking);
        loyaltyPointService.addPointsFromBooking(booking.getUser(), booking);
        notificationService.notifyBookingPaid(booking);
        log.info("Payment {} confirmed for booking {}", payment.getId(), booking.getBookingCode());
    }

    private void generateSeatTickets(Booking booking) {
        bookingSeatRepository.findByBooking(booking)
                .stream()
                .filter(seat -> seat.getTicketCode() == null)
                .forEach(seat -> {
                    seat.setTicketCode(qrTicketService.generateTicketCode(seat));
                    seat.setQrCode(qrTicketService.generateSeatQr(seat));
                });
    }

    private void markPendingPayment(Booking booking) {
        requireBookingStatus(booking, BookingStatus.HOLDING, "Only holding booking can move to pending payment");
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
    }

    private void markPaid(Booking booking, String qrCode) {
        requireBookingStatus(booking, BookingStatus.PENDING_PAYMENT, "Only pending payment booking can be marked as paid");
        booking.setQrCode(qrCode);
        booking.setPaidAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.PAID);
    }

    private void markPaymentSuccess(Payment payment, String transactionId, String callbackPayload, Map<String, String> params) {
        payment.setTransactionId(transactionId);
        payment.setCallbackPayload(callbackPayload);
        payment.setPaidAt(LocalDateTime.now());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentAccountLabel(PaymentAccountSupport.resolveFromCallback(payment.getProvider(), params));
    }

    private void markPaymentFailed(Payment payment, String callbackPayload) {
        payment.setCallbackPayload(callbackPayload);
        payment.setStatus(PaymentStatus.FAILED);
    }

    /** Hủy các payment VÉ (không tính food order) đang PENDING của booking khi user retry. */
    private void supersedePendingTicketPayments(Booking booking) {
        paymentRepository.findByBooking(booking)
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING && p.getFoodOrder() == null)
                .forEach(p -> {
                    log.info("Superseding pending payment {} of booking {} (retry)", p.getId(), booking.getBookingCode());
                    markPaymentFailed(p, "SUPERSEDED_BY_RETRY");
                });
    }

    private void changeBookingSeatStatus(BookingSeat bookingSeat, SeatRuntimeStatus status) {
        if (status == null) {
            throw new BadRequestException("Seat runtime status is required");
        }
        bookingSeat.setStatus(status);
    }

    private void requireBookingStatus(Booking booking, BookingStatus expectedStatus, String message) {
        if (booking.getStatus() != expectedStatus) {
            throw new BadRequestException(message);
        }
    }

    private Payment findPaymentByTxnRef(String txnRef) {
        if (txnRef == null) return null;
        String[] parts = txnRef.split("-", 2);
        try {
            Long paymentId = Long.parseLong(parts[0]);
            return paymentRepository.findById(paymentId).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private void validateBookingOwner(Booking booking, String email) {
        if (!booking.getUser().getEmail().equals(email)) {
            throw new NotFoundException("Booking not found");
        }
    }

    private void validateFoodOrderOwner(FoodOrder foodOrder, String email) {
        String ownerEmail = foodOrder.getCustomer() != null
                ? foodOrder.getCustomer().getEmail()
                : foodOrder.getBooking() != null ? foodOrder.getBooking().getUser().getEmail() : null;
        if (!email.equals(ownerEmail)) {
            throw new NotFoundException("Food order not found");
        }
    }

    private PaymentResponse toResponse(Payment payment, String paymentUrl) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking() == null ? null : payment.getBooking().getId(),
                payment.getProvider(),
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getStatus(),
                paymentUrl,
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }

    private String toJson(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return params.toString();
        }
    }
}
