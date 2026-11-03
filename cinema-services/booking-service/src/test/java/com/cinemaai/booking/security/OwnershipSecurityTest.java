package com.cinemaai.booking.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.OrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OwnershipSecurityTest {
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);

    @Test
    void bookingOwnershipRequiresBothIdsAndMatchingOwner() {
        BookingSecurity security = new BookingSecurity(bookingRepository);
        when(bookingRepository.findByIdAndUserId(10L, 20L)).thenReturn(Optional.of(mock(com.cinemaai.booking.entity.Booking.class)));

        assertTrue(security.isOwner(10L, 20L));
        assertFalse(security.isOwner(10L, 21L));
        assertFalse(security.isOwner(null, 20L));
        assertFalse(security.isOwner(10L, null));
        verify(bookingRepository, never()).findByIdAndUserId(null, 20L);
    }

    @Test
    void orderOwnershipRequiresBothIdsAndMatchingOwner() {
        OrderSecurity security = new OrderSecurity(orderRepository);
        when(orderRepository.findByIdAndUserId(30L, 40L)).thenReturn(Optional.of(mock(com.cinemaai.booking.entity.Order.class)));

        assertTrue(security.isOwner(30L, 40L));
        assertFalse(security.isOwner(30L, 41L));
        assertFalse(security.isOwner(null, 40L));
        assertFalse(security.isOwner(30L, null));
        verify(orderRepository, never()).findByIdAndUserId(null, 40L);
    }
}
