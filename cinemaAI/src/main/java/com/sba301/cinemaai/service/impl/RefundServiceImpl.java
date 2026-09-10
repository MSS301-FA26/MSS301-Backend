package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.CineWallet;
import com.sba301.cinemaai.entity.Payment;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.WalletTransaction;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.PaymentStatus;
import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import com.sba301.cinemaai.enums.WalletTransactionType;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.CineWalletRepository;
import com.sba301.cinemaai.repository.PaymentRepository;
import com.sba301.cinemaai.repository.WalletTransactionRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.LoyaltyPointService;
import com.sba301.cinemaai.service.MailService;
import com.sba301.cinemaai.service.NotificationService;
import com.sba301.cinemaai.service.RefundService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final CineWalletRepository cineWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final LoyaltyPointService loyaltyPointService;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public void processShowtimeCancellation(Showtime showtime, String reason) {
        String refundReason = buildRefundReason(reason);

        bookingRepository.findByShowtime(showtime).forEach(booking -> {
            switch (booking.getStatus()) {
                case HOLDING, PENDING_PAYMENT -> {
                    cancelUnpaidBooking(booking);
                    notificationService.notifyBookingCancelled(booking);
                }
                case PAID -> processPaidBookingRefund(booking, showtime, refundReason);
                default -> { /* terminal or already refunded — no action */ }
            }
        });
    }

    @Override
    @Transactional
    public void refundPaidBooking(Booking booking, String reason) {
        applyPaidRefund(booking, buildBookingRefundReason(reason), false);
        // Thông báo chung cho việc hủy vé lẻ (tránh gửi nhầm "suất chiếu bị hủy")
        notificationService.notifyBookingCancelled(booking);
    }

    private void processPaidBookingRefund(Booking booking, Showtime showtime, String refundReason) {
        BigDecimal refundAmount = applyPaidRefund(booking, refundReason, true);
        // Thông báo đặc thù cho hủy suất chiếu
        notificationService.notifyShowtimeCancelled(booking.getUser(), booking, showtime);
        log.info("Refunded {} VND to CineWallet for booking {} due to showtime cancellation",
                refundAmount, booking.getBookingCode());
    }

    /**
     * Lõi hoàn tiền một vé PAID: ghi có CineWallet, ghi sổ giao dịch, giải phóng ghế,
     * cập nhật booking + payment, gửi email, ghi audit. KHÔNG gửi push notification
     * (để mỗi caller chọn thông điệp phù hợp). Idempotent theo booking.
     *
     * @return số tiền đã hoàn (totalAmount của vé)
     */
    private BigDecimal applyPaidRefund(Booking booking, String refundReason, boolean bulk) {
        BigDecimal refundAmount = booking.getTotalAmount();

        // 1. Hoàn điểm loyalty
        loyaltyPointService.restoreRedeemedPointsFromBooking(booking.getUser(), booking);
        loyaltyPointService.revokePointsFromBooking(booking.getUser(), booking);

        // 2. Hoàn tiền vào CineWallet
        CineWallet wallet = cineWalletRepository.findByUserForUpdate(booking.getUser())
                .orElseGet(() -> cineWalletRepository.save(new CineWallet(booking.getUser())));

        BigDecimal newBalance = wallet.getBalance().add(refundAmount);
        wallet.setBalance(newBalance);
        cineWalletRepository.save(wallet);

        // 3. Tạo giao dịch wallet (idempotent theo booking)
        String refCode = "REFUND-" + booking.getBookingCode();
        if (!walletTransactionRepository.existsByBookingAndType(booking, WalletTransactionType.REFUND_CREDIT)) {
            walletTransactionRepository.save(new WalletTransaction(
                    wallet,
                    booking.getUser(),
                    booking,
                    WalletTransactionType.REFUND_CREDIT,
                    refundAmount,
                    newBalance,
                    refCode,
                    refundReason
            ));
        }

        // 4. Giải phóng ghế và cập nhật booking
        releaseSeats(booking);
        booking.setQrCode(null);
        booking.setStatus(BookingStatus.REFUNDED);
        booking.setRefundedAt(LocalDateTime.now());
        booking.setRefundReason(refundReason);
        booking.setRefundMethod("CINEWALLET");
        booking.setBulkRefund(bulk);
        bookingRepository.save(booking);

        // 5. Cập nhật payment
        Payment payment = paymentRepository.findFirstByBookingIdAndStatusAndFoodOrderIsNullOrderByIdDesc(booking.getId(), PaymentStatus.SUCCESS)
                .orElse(null);
        if (payment != null) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundAmount(refundAmount);
            payment.setRefundedAt(LocalDateTime.now());
            payment.setRefundTransactionNo(refCode);
            payment.setRefundMethod("CINEWALLET");
            payment.setPaymentAccountLabel(null);
            payment.setCallbackPayload(null);
            paymentRepository.save(payment);
        }

        // 6. Gửi email
        mailService.sendWalletRefundNotice(
                booking.getUser().getEmail(),
                booking.getBookingCode(),
                refundAmount,
                newBalance,
                refundReason
        );

        log.info("Refunded {} VND to CineWallet for booking {} (new balance: {})",
                refundAmount, booking.getBookingCode(), newBalance);
        auditLogService.record(AuditActionType.REFUND, "BOOKING", booking.getId(),
                booking.getBookingCode() + " - " + refundAmount + " VND");

        return refundAmount;
    }

    private String buildBookingRefundReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Admin hoàn/hủy vé";
        }
        return "Admin hoàn/hủy vé: " + reason.trim();
    }

    private void cancelUnpaidBooking(Booking booking) {
        releaseSeats(booking);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.CANCELLED);
    }

    private void releaseSeats(Booking booking) {
        bookingSeatRepository.findByBooking(booking)
                .forEach(seat -> seat.setStatus(SeatRuntimeStatus.RELEASED));
    }

    private String buildRefundReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Showtime cancelled due to operational incident";
        }
        return "Hủy suất chiếu do sự cố: " + reason.trim();
    }
}
