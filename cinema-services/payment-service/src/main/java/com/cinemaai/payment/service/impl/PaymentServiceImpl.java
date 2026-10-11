package com.cinemaai.payment.service.impl;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.dto.response.PaymentResponse;
import com.cinemaai.payment.dto.response.VNPayPaymentResponse;
import com.cinemaai.payment.entity.OutboxEvent;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.OutboxStatus;
import com.cinemaai.payment.enums.PaymentProvider;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.event.PaymentSucceededEvent;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.exception.NotFoundException;
import com.cinemaai.payment.mapper.PaymentMapper;
import com.cinemaai.payment.repository.OutboxEventRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.service.OutboxPublisherWorker;
import com.cinemaai.payment.service.PaymentService;
import com.cinemaai.payment.service.LoyaltyService;
import com.cinemaai.payment.util.VNPayUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisherWorker outboxPublisherWorker;
    private final BookingClient bookingClient;
    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper;

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.pay-url}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Override
    @Transactional
    public VNPayPaymentResponse createVnpayPayment(Long userId, Long bookingId, Long foodOrderId, String clientIp) {
        BigDecimal amount = BigDecimal.ZERO;
        Long targetUserId = userId;

        if (bookingId != null) {
            BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
            amount = booking.totalAmount();
            if (targetUserId == null) {
                targetUserId = booking.userId();
            }
        } else if (foodOrderId != null) {
            BookingClient.FoodOrderInfo foodOrder = bookingClient.getFoodOrder(foodOrderId);
            amount = foodOrder.totalAmount();
            if (targetUserId == null) {
                targetUserId = foodOrder.userId();
            }
        } else {
            throw new BadRequestException("Phải chỉ định bookingId hoặc foodOrderId để thanh toán.");
        }

        // Create or reuse pending payment
        Payment payment = (bookingId != null)
                ? paymentRepository.findByBookingId(bookingId).filter(p -> p.getStatus() == PaymentStatus.PENDING).orElse(null)
                : paymentRepository.findByFoodOrderId(foodOrderId).filter(p -> p.getStatus() == PaymentStatus.PENDING).orElse(null);

        if (payment == null) {
            payment = Payment.builder()
                    .bookingId(bookingId)
                    .foodOrderId(foodOrderId)
                    .userId(targetUserId != null ? targetUserId : 1L)
                    .provider(PaymentProvider.VNPAY)
                    .amount(amount)
                    .status(PaymentStatus.PENDING)
                    .build();
            payment = paymentRepository.save(payment);
        } else {
            payment.setAmount(amount);
            payment = paymentRepository.save(payment);
        }

        // Generate VNPay URL
        String orderRefCode = null;
        String orderInfo = null;
        if (bookingId != null) {
            BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
            orderRefCode = booking.bookingCode();
            orderInfo = "Thanh toan ve xem phim ma " + orderRefCode;
        } else if (foodOrderId != null) {
            BookingClient.FoodOrderInfo foodOrder = bookingClient.getFoodOrder(foodOrderId);
            orderRefCode = foodOrder.foodOrderCode();
            orderInfo = "Thanh toan bap nuoc ma " + orderRefCode;
        }
        String txnRef = payment.getId() + "-" + (orderRefCode != null && !orderRefCode.isBlank() ? orderRefCode : System.currentTimeMillis());

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", clientIp != null && !clientIp.isBlank() ? clientIp : "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!hashData.isEmpty()) {
                    hashData.append('&');
                    query.append('&');
                }
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
        }

        String vnpSecureHash = VNPayUtil.hmacSHA512(vnpHashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);
        String paymentUrl = vnpPayUrl + "?" + query;

        payment.setPaymentUrl(paymentUrl);
        paymentRepository.save(payment);

        return new VNPayPaymentResponse(payment.getId(), paymentUrl, txnRef, amount, "VNPAY");
    }

    @Override
    @Transactional
    public PaymentResponse mockPayment(Long userId, Long bookingId, Long foodOrderId) {
        BigDecimal amount = BigDecimal.ZERO;
        Long targetUserId = userId;

        if (bookingId != null) {
            BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
            amount = booking.totalAmount();
            if (targetUserId == null) {
                targetUserId = booking.userId();
            }
        } else if (foodOrderId != null) {
            BookingClient.FoodOrderInfo foodOrder = bookingClient.getFoodOrder(foodOrderId);
            amount = foodOrder.totalAmount();
            if (targetUserId == null) {
                targetUserId = foodOrder.userId();
            }
        } else {
            throw new BadRequestException("Phải chỉ định bookingId hoặc foodOrderId để thanh toán.");
        }

        Payment payment = (bookingId != null)
                ? paymentRepository.findByBookingId(bookingId).orElse(null)
                : paymentRepository.findByFoodOrderId(foodOrderId).orElse(null);
        if (payment == null) {
            payment = Payment.builder()
                    .bookingId(bookingId)
                    .foodOrderId(foodOrderId)
                    .userId(targetUserId != null ? targetUserId : 1L)
                    .provider(PaymentProvider.MOCK)
                    .amount(amount)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(now);
        payment.setTransactionId("MOCK-" + System.currentTimeMillis());
        payment.setPaymentAccountLabel("MOCK WALLET");
        payment = paymentRepository.save(payment);

        // Transactional Outbox: Write PaymentSucceededEvent in the same DB transaction
        writeOutboxEvent(payment, now);
        triggerOutboxPublishImmediately();

        if (payment.getBookingId() != null) {
            bookingClient.markBookingPaid(payment.getBookingId(), payment.getTransactionId(), now);
            try {
                var bInfo = bookingClient.getBooking(payment.getBookingId());
                if (bInfo != null && bInfo.userId() != null) {
                    loyaltyService.awardPointsForBooking(new com.cinemaai.payment.dto.request.AwardBookingPointsRequest(
                            bInfo.userId(),
                            bInfo.id(),
                            bInfo.bookingCode(),
                            payment.getAmount(),
                            bInfo.loyaltyPointsRedeemed() != null ? bInfo.loyaltyPointsRedeemed() : 0,
                            bInfo.cinemaId()
                    ));
                }
            } catch (Exception ex) {
                log.warn("Failed to award loyalty points for booking {} in mockPayment: {}", payment.getBookingId(), ex.getMessage());
            }
        }
        if (payment.getFoodOrderId() != null) {
            bookingClient.markFoodOrderPaid(payment.getFoodOrderId(), payment.getTransactionId(), now);
            try {
                var foInfo = bookingClient.getFoodOrder(payment.getFoodOrderId());
                if (foInfo != null && foInfo.userId() != null) {
                    loyaltyService.awardPointsForFoodOrder(
                            foInfo.userId(),
                            foInfo.id(),
                            foInfo.foodOrderCode(),
                            payment.getAmount()
                    );
                }
            } catch (Exception ex) {
                log.warn("Failed to award loyalty points for food order {} in mockPayment: {}", payment.getFoodOrderId(), ex.getMessage());
            }
        }

        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public Map<String, String> processVnpayIpn(Map<String, String> params) {
        Map<String, String> fields = new HashMap<>(params);
        String secureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String signValue = VNPayUtil.hashAllFields(fields, vnpHashSecret);
        if (secureHash == null || !secureHash.equalsIgnoreCase(signValue)) {
            log.warn("VNPay IPN Invalid Checksum. Expected: {}, Got: {}", signValue, secureHash);
            return Map.of("RspCode", "97", "Message", "Invalid Checksum");
        }

        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        Long paymentId;
        try {
            paymentId = Long.parseLong(txnRef.split("[-_]")[0]);
        } catch (Exception ex) {
            return Map.of("RspCode", "01", "Message", "Invalid transaction reference");
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        // Amount check
        long vnpAmount = Long.parseLong(params.getOrDefault("vnp_Amount", "0"));
        long expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (vnpAmount != expectedAmount) {
            return Map.of("RspCode", "04", "Message", "Invalid Amount");
        }

        // Idempotency check: Already processed
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (payment.getBookingId() != null) {
                bookingClient.markBookingPaid(payment.getBookingId(), payment.getTransactionId(), payment.getPaidAt());
            }
            if (payment.getFoodOrderId() != null) {
                bookingClient.markFoodOrderPaid(payment.getFoodOrderId(), payment.getTransactionId(), payment.getPaidAt());
            }
            return Map.of("RspCode", "00", "Message", "Order already confirmed");
        }

        String responseCode = params.get("vnp_ResponseCode");
        LocalDateTime now = LocalDateTime.now();

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(now);
            payment.setTransactionId(params.get("vnp_TransactionNo"));
            payment.setPaymentAccountLabel("VNPAY " + params.getOrDefault("vnp_BankCode", ""));
            payment.setCallbackPayload(params.toString());
            paymentRepository.save(payment);

            // Transactional Outbox pattern: Record event in the same DB transaction
            writeOutboxEvent(payment, now);
            triggerOutboxPublishImmediately();

            if (payment.getBookingId() != null) {
                bookingClient.markBookingPaid(payment.getBookingId(), payment.getTransactionId(), now);
                try {
                    var bInfo = bookingClient.getBooking(payment.getBookingId());
                    if (bInfo != null && bInfo.userId() != null) {
                        loyaltyService.awardPointsForBooking(new com.cinemaai.payment.dto.request.AwardBookingPointsRequest(
                                bInfo.userId(),
                                bInfo.id(),
                                bInfo.bookingCode(),
                                payment.getAmount(),
                                bInfo.loyaltyPointsRedeemed() != null ? bInfo.loyaltyPointsRedeemed() : 0
                        ));
                    }
                } catch (Exception ex) {
                    log.warn("Failed to award loyalty points for booking {} in VNPay IPN: {}", payment.getBookingId(), ex.getMessage());
                }
            }
            if (payment.getFoodOrderId() != null) {
                bookingClient.markFoodOrderPaid(payment.getFoodOrderId(), payment.getTransactionId(), now);
                try {
                    var foInfo = bookingClient.getFoodOrder(payment.getFoodOrderId());
                    if (foInfo != null && foInfo.userId() != null) {
                        loyaltyService.awardPointsForFoodOrder(
                                foInfo.userId(),
                                foInfo.id(),
                                foInfo.foodOrderCode(),
                                payment.getAmount()
                        );
                    }
                } catch (Exception ex) {
                    log.warn("Failed to award loyalty points for food order {} in VNPay IPN: {}", payment.getFoodOrderId(), ex.getMessage());
                }
            }

            log.info("VNPay IPN Success for Payment id={}, Booking id={}, FoodOrder id={}", payment.getId(), payment.getBookingId(), payment.getFoodOrderId());
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setCallbackPayload(params.toString());
            paymentRepository.save(payment);
            log.warn("VNPay IPN Failed for Payment id={}, code={}", payment.getId(), responseCode);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }
    }

    private void triggerOutboxPublishImmediately() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        outboxPublisherWorker.publishPendingEvents();
                    } catch (Exception ex) {
                        log.warn("Immediate outbox publish failed (scheduled worker will retry): {}", ex.getMessage());
                    }
                }
            });
        } else {
            try {
                outboxPublisherWorker.publishPendingEvents();
            } catch (Exception ex) {
                log.warn("Immediate outbox publish failed: {}", ex.getMessage());
            }
        }
    }

    private void writeOutboxEvent(Payment payment, LocalDateTime paidAt) {
        try {
            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    UUID.randomUUID().toString(),
                    payment.getId(),
                    payment.getBookingId(),
                    payment.getFoodOrderId(),
                    payment.getUserId(),
                    payment.getAmount(),
                    payment.getProvider().name(),
                    paidAt
            );

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("PAYMENT")
                    .aggregateId(String.valueOf(payment.getId()))
                    .eventType("PaymentSucceededEvent")
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
        } catch (Exception ex) {
            log.error("Failed to serialize OutboxEvent: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch thanh toán cho booking: " + bookingId));
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByFoodOrder(Long foodOrderId) {
        Payment payment = paymentRepository.findByFoodOrderId(foodOrderId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay giao dich thanh toan cho don bap nuoc: " + foodOrderId));
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch thanh toán id: " + paymentId));
        return PaymentMapper.toResponse(payment);
    }
}
