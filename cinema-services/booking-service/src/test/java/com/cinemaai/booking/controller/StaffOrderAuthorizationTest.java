package com.cinemaai.booking.controller;

import com.cinemaai.booking.entity.Order;
import com.cinemaai.booking.exception.ForbiddenException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.OrderRepository;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.security.CinemaSecurityService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffOrderAuthorizationTest {
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingSeatRepository seats = mock(BookingSeatRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CinemaSecurityService cinemaSecurity = mock(CinemaSecurityService.class);
    private final BookingMapper mapper = mock(BookingMapper.class);
    private StaffCheckInController controller;

    @BeforeEach
    void setUp() {
        controller = new StaffCheckInController(bookings, seats, orders, cinemaSecurity, mapper);
    }

    @Test
    void lookupRejectsStaffFromAnotherCinema() {
        AuthenticatedUser staff = staffAt(101L);
        when(orders.findByFoodOrderCode("FO-8")).thenReturn(Optional.of(order(8L, 202L, "PAID")));
        doThrow(new ForbiddenException("wrong cinema"))
                .when(cinemaSecurity).validateCinemaAccess(staff, 202L, true);

        assertThrows(ForbiddenException.class, () -> controller.lookupFoodOrder(staff, "FO-8"));
    }

    @Test
    void pickupRejectsStaffFromAnotherCinemaWithoutChangingState() {
        AuthenticatedUser staff = staffAt(101L);
        Order order = order(8L, 202L, "PAID");
        when(orders.findByFoodOrderCode("FO-8")).thenReturn(Optional.of(order));
        doThrow(new ForbiddenException("wrong cinema"))
                .when(cinemaSecurity).validateCinemaAccess(staff, 202L, true);

        assertThrows(ForbiddenException.class,
                () -> controller.pickUpFoodOrder(staff, Map.of("code", "FO-8")));

        assertEquals("PAID", order.getStatus());
        verify(orders, never()).save(any());
    }

    @Test
    void authorizedStaffCanLookupAndPickupOrderAtAssignedCinema() {
        AuthenticatedUser staff = staffAt(101L);
        Order order = order(8L, 101L, "PAID");
        when(orders.findByFoodOrderCode("FO-8")).thenReturn(Optional.of(order));

        assertEquals(8L, controller.lookupFoodOrder(staff, "FO-8").data().get("id"));
        assertEquals("PICKED_UP", controller.pickUpFoodOrder(staff, Map.of("code", "FO-8")).data().get("status"));
        assertEquals("PICKED_UP", order.getStatus());
        verify(cinemaSecurity, times(2)).validateCinemaAccess(staff, 101L, true);
        verify(orders).save(order);
    }

    private AuthenticatedUser staffAt(Long cinemaId) {
        return new AuthenticatedUser(5L, "staff@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF")), cinemaId);
    }

    private Order order(Long id, Long cinemaId, String status) {
        return Order.builder().id(id).foodOrderCode("FO-8").userId(20L)
                .cinemaId(cinemaId).status(status).build();
    }
}
