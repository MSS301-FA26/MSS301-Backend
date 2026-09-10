package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.notification.NotificationCreateRequest;
import com.sba301.cinemaai.dto.response.notification.NotificationResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.User;
import java.util.List;

public interface NotificationService {

        NotificationResponse createForUser(NotificationCreateRequest request);

        List<NotificationResponse> getMyNotifications(Long userId);

        List<NotificationResponse> getMyUnread(Long userId);

        NotificationResponse markRead(Long userId, Long notificationId);

        int markAllRead(Long userId);

        void notifyBookingPaid(Booking booking);

        void notifyBookingCancelled(Booking booking);

        void notifyShowtimeCancelled(User user, Booking booking, Showtime showtime);
}
