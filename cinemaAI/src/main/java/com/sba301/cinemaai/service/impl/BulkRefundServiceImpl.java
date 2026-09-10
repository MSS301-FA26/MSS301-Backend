package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.response.refund.BulkRefundResponse;
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
import com.sba301.cinemaai.service.BulkRefundService;
import com.sba301.cinemaai.service.LoyaltyPointService;
import com.sba301.cinemaai.service.MailService;
import com.sba301.cinemaai.service.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkRefundServiceImpl implements BulkRefundService {

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
    public BulkRefundResponse processBulkRefund(Showtime showtime, String reason) {
        String refundReason = buildRefundReason(reason);
        List<Booking> bookings = bookingRepository.findByShowtimeIdAndStatusInForUpdate(
                showtime.getId(),
                java.util.List.of(BookingStatus.PAID, BookingStatus.USED)
        );
        int successCount = 0;

        for (Booking booking : bookings) {
            try {
                refundToWallet(booking, showtime, refundReason);
                successCount++;
            } catch (Exception e) {
                log.error("Error refunding booking {} to wallet", booking.getBookingCode(), e);
            }
        }

        auditLogService.record(AuditActionType.REFUND, "SHOWTIME", showtime.getId(),
                "Suất chiếu #" + showtime.getId() + " - hoàn tiền " + successCount + "/" + bookings.size() + " booking");

        return new BulkRefundResponse(
                showtime.getId(),
                bookings.size(),
                successCount,
                bookings.size() - successCount,
                0,
                "COMPLETED",
                "Completed"
        );
    }

    private void refundToWallet(Booking booking, Showtime showtime, String refundReason) {
        BigDecimal refundAmount = booking.getTotalAmount();

        // 1. Hoàn điểm loyalty
        loyaltyPointService.restoreRedeemedPointsFromBooking(booking.getUser(), booking);
        loyaltyPointService.revokePointsFromBooking(booking.getUser(), booking);

        // 2. Hoàn tiền vào CineWallet (lazy-create nếu chưa có ví)
        CineWallet wallet = cineWalletRepository.findByUserForUpdate(booking.getUser())
                .orElseGet(() -> cineWalletRepository.save(new CineWallet(booking.getUser())));

        BigDecimal newBalance = wallet.getBalance().add(refundAmount);
        wallet.setBalance(newBalance);
        cineWalletRepository.save(wallet);

        // 3. Tạo giao dịch wallet
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
        booking.setBulkRefund(true);
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

        // 6. Gửi thông báo + email
        notificationService.notifyShowtimeCancelled(booking.getUser(), booking, showtime);
        mailService.sendWalletRefundNotice(
                booking.getUser().getEmail(),
                booking.getBookingCode(),
                refundAmount,
                newBalance,
                refundReason
        );

        log.info("Bulk refund: {} VND → CineWallet for booking {} (balance: {})",
                refundAmount, booking.getBookingCode(), newBalance);
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
