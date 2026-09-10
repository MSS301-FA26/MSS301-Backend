package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.notification.NotificationCreateRequest;
import com.sba301.cinemaai.dto.response.notification.NotificationResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Notification;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.NotificationType;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.NotificationRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public NotificationResponse createForUser(NotificationCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getUserId()));
        Notification notification = new Notification(user, request.getTitle(), request.getMessage(), request.getType());
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    @Override
    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<NotificationResponse> getMyUnread(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    @Override
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return NotificationResponse.from(notification);
        }
        notification.setIsRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    @Override
    public int markAllRead(Long userId) {
        return notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    @Override
    public void notifyBookingPaid(Booking booking) {
        String movieTitle = booking.getShowtime().getMovie().getTitle();
        String title = "Đặt vé thành công";
        String message = String.format("Booking %s cho phim \"%s\" đã thanh toán thành công. Mã QR của bạn đã sẵn sàng.",
                booking.getBookingCode(), movieTitle);
        saveNotification(booking.getUser(), title, message, NotificationType.PAYMENT);
    }

    @Transactional
    @Override
    public void notifyBookingCancelled(Booking booking) {
        String movieTitle = booking.getShowtime().getMovie().getTitle();
        String title = "Booking đã bị hủy";
        String message = String.format("Booking %s cho phim \"%s\" đã bị hủy.",
                booking.getBookingCode(), movieTitle);
        saveNotification(booking.getUser(), title, message, NotificationType.BOOKING);
    }

    @Transactional
    @Override
    public void notifyShowtimeCancelled(User user, Booking booking, Showtime showtime) {
        String movieTitle = showtime.getMovie().getTitle();
        String title = "Suất chiếu bị hủy — hoàn tiền";
        String message = String.format(
                "Suất chiếu phim \"%s\" lúc %s đã bị rạp hủy do sự cố. Booking %s của bạn đã được hoàn tiền vào ví.",
                movieTitle, showtime.getStartTime(), booking.getBookingCode());
        saveNotification(user, title, message, NotificationType.BOOKING);
    }

    private void saveNotification(User user, String title, String message, NotificationType type) {
        try {
            notificationRepository.save(new Notification(user, title, message, type));
        } catch (Exception e) {
            log.warn("Failed to save notification for user {}: {}", user.getId(), e.getMessage());
        }
    }
}
