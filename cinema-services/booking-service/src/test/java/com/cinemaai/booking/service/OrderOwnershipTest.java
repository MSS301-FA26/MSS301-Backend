package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.dto.request.OrderRequest;
import com.cinemaai.booking.dto.response.OrderResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.Order;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.mapper.OrderMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.OrderRepository;
import com.cinemaai.booking.service.impl.OrderServiceImpl;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class OrderOwnershipTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PaymentClient payments = mock(PaymentClient.class);
    private final OrderMapper mapper = mock(OrderMapper.class);
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orders, bookings, mock(CatalogClient.class), payments, mapper);
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
                () -> service.createForBooking(1L, 7L, new OrderRequest(List.of())));
        verifyNoInteractions(payments);
        verify(orders, never()).save(any());
    }

    @Test
    void userCannotCancelAnotherUsersOrder() {
        when(orders.findByIdAndUserId(8L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.cancel(1L, 8L));
        verifyNoInteractions(payments);
        verify(orders, never()).save(any());
    }

    @Test
    void userCannotSyncAnotherUsersOrder() {
        when(orders.findByIdAndUserId(8L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.syncPaymentStatus(1L, 8L));
        verifyNoInteractions(payments);
        verify(orders, never()).save(any());
    }

    @Test
    void listMineUsesRepositoryOwnerFilterAndReturnsOnlyOwnedOrders() {
        Order owned = order(8L, 1L, "PAID");
        OrderResponse response = mock(OrderResponse.class);
        when(orders.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(owned));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(eq(owned), nullable(String.class), any(), nullable(String.class), anyList()))
                .thenReturn(response);

        assertEquals(List.of(response), service.listMine(1L));
        verify(orders).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void listMineExpiresOnlyOverduePendingOrders() {
        Order expired = order(8L, 1L, "PENDING_PAYMENT");
        setField(expired, "createdAt", LocalDateTime.now().minusMinutes(16));
        Order active = order(9L, 1L, "PENDING_PAYMENT");
        setField(active, "createdAt", LocalDateTime.now().minusMinutes(14));
        Order paid = order(10L, 1L, "PAID");
        setField(paid, "createdAt", LocalDateTime.now().minusHours(1));
        when(orders.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(expired, active, paid));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());

        service.listMine(1L);

        assertEquals("EXPIRED", expired.getStatus());
        assertEquals("PENDING_PAYMENT", active.getStatus());
        assertEquals("PAID", paid.getStatus());
        verify(orders).findByUserIdOrderByCreatedAtDesc(1L);
        verify(orders, never()).findAll();
    }

    @Test
    void listMineReturnsEmptyListForUserWithoutOrders() {
        when(orders.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        assertEquals(List.of(), service.listMine(1L));
    }

    private Order order(Long id, Long userId, String status) {
        return Order.builder().id(id).foodOrderCode("FO-8").userId(userId).status(status).build();
    }
}
