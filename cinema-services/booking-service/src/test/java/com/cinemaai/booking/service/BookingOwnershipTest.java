package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingFoodItemRepository;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.BookingTicketRepository;
import com.cinemaai.booking.service.impl.BookingServiceImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingOwnershipTest {
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingMapper mapper = mock(BookingMapper.class);
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookings, mock(BookingSeatRepository.class),
                mock(BookingTicketRepository.class), mock(BookingFoodItemRepository.class),
                mock(CatalogClient.class), mock(PaymentClient.class), mapper);
    }

    @Test
    void foreignBookingCodeIsHiddenFromUser() {
        when(bookings.findByBookingCodeAndUserId("BK-B", 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getBookingByCode(1L, "BK-B"));
        verify(bookings, never()).findByBookingCode("BK-B");
    }

    @Test
    void ownerCanReadBookingByCode() {
        Booking booking = booking(7L, 1L, "BK-A");
        BookingResponse response = mock(BookingResponse.class);
        when(bookings.findByBookingCodeAndUserId("BK-A", 1L)).thenReturn(Optional.of(booking));
        when(mapper.toResponse(booking)).thenReturn(response);

        assertSame(response, service.getBookingByCode(1L, "BK-A"));
    }

    @Test
    void internalLookupStillReadsByIdWithoutUserOwnershipFilter() {
        Booking booking = booking(7L, 2L, "BK-B");
        BookingResponse response = mock(BookingResponse.class);
        when(bookings.findById(7L)).thenReturn(Optional.of(booking));
        when(mapper.toResponse(booking)).thenReturn(response);

        assertSame(response, service.getBookingByIdInternal(7L));
        verify(bookings).findById(7L);
    }

    private Booking booking(Long id, Long userId, String code) {
        return Booking.builder().id(id).userId(userId).bookingCode(code).status(BookingStatus.PAID)
                .holdExpiresAt(LocalDateTime.now().plusMinutes(1)).build();
    }
}
