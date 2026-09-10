package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.PaymentProvider;
import com.sba301.cinemaai.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_order_id")
    private FoodOrder foodOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Setter
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Setter
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Setter
    @Column(name = "callback_payload", columnDefinition = "TEXT")
    private String callbackPayload;

    @Setter
    @Column(name = "payment_account_label", length = 100)
    private String paymentAccountLabel;

    @Setter
    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount; // Số tiền thực tế bồi thường (Bằng booking.totalAmount)

    @Setter
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt; // Thời điểm giao dịch hoàn tiền hoàn tất

    @Setter
    @Column(name = "refund_transaction_no", length = 100)
    private String refundTransactionNo; // Mã đối soát của VNPay hoặc mã Staff xử lý tay

    @Setter
    @Column(name = "refund_method", length = 50)
    private String refundMethod;

    public Payment(Booking booking, PaymentProvider provider, BigDecimal amount) {
        this.booking = booking;
        this.provider = provider;
        this.amount = amount;
    }
}
