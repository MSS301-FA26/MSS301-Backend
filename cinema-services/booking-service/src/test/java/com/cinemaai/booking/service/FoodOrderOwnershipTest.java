package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.dto.request.FoodOrderRequest;
import com.cinemaai.booking.dto.response.FoodOrderResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.FoodOrder;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.mapper.FoodOrderMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.FoodOrderRepository;
import com.cinemaai.booking.service.impl.FoodOrderServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FoodOrderOwnershipTest {
    private final FoodOrderRepository orders = mock(FoodOrderRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PaymentClient payments = mock(PaymentClient.class);
    private final FoodOrderMapper mapper = mock(FoodOrderMapper.class);
    private FoodOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FoodOrderServiceImpl(orders, bookings, mock(CatalogClient.class), payments, mapper);
    }

    @Test
    void userCannotReadAnotherUsersFoodOrder() {
        when(orders.findByIdAndUserId(8L, 1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getByIdAndUser(1L, 8L));
    }

    @Test
    void userCannotCreateFoodOrderForAnotherUsersBooking() {
        when(bookings.findById(7L)).thenReturn(Optional.of(
                Booking.builder().id(7L).userId(2L).status(BookingStatus.PAID).build()));

        assertThrows(NotFoundException.class,
                () -> service.createForBooking(1L, 7L, new FoodOrderRequest(List.of())));
        verifyNoInteractions(payments);
        verify(orders, never()).save(any());
    }

    @Test
    void userCannotCancelOrSyncAnotherUsersOrder() {
        FoodOrder other = order(8L, 2L, "PENDING_PAYMENT");
        when(orders.findById(8L)).thenReturn(Optional.of(other));

        assertThrows(BadRequestException.class, () -> service.cancel(1L, 8L));
        assertThrows(BadRequestException.class, () -> service.syncPaymentStatus(1L, 8L));
        assertEquals("PENDING_PAYMENT", other.getStatus());
        verifyNoInteractions(payments);
        verify(orders, never()).save(any());
    }

    @Test
    void listMineUsesRepositoryOwnerFilterAndReturnsOnlyOwnedOrders() {
        FoodOrder owned = order(8L, 1L, "PAID");
        FoodOrderResponse response = mock(FoodOrderResponse.class);
        when(orders.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(owned));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(eq(owned), nullable(String.class), any(), nullable(String.class), anyList()))
                .thenReturn(response);

        assertEquals(List.of(response), service.listMine(1L));
        verify(orders).findByUserIdOrderByCreatedAtDesc(1L);
    }

    private FoodOrder order(Long id, Long userId, String status) {
        return FoodOrder.builder().id(id).foodOrderCode("FO-8").userId(userId).status(status).build();
    }
}
