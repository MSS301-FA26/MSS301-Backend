package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.dto.response.FoodOrderResponse;
import com.cinemaai.booking.dto.response.PageResponse;
import com.cinemaai.booking.entity.FoodOrder;
import com.cinemaai.booking.mapper.FoodOrderMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.FoodOrderRepository;
import com.cinemaai.booking.service.impl.FoodOrderServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOrderListingTest {

    @Test
    void adminListingReturnsOrdersAcrossUsersWithStableServerSorting() {
        FoodOrderRepository orders = mock(FoodOrderRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        FoodOrderMapper mapper = mock(FoodOrderMapper.class);
        FoodOrderServiceImpl service = new FoodOrderServiceImpl(
                orders, bookings, mock(CatalogClient.class), mock(PaymentClient.class), mapper);

        FoodOrder first = FoodOrder.builder().id(20L).userId(1L).foodOrderCode("FO-20").status("PAID").build();
        FoodOrder second = FoodOrder.builder().id(19L).userId(2L).foodOrderCode("FO-19").status("PAID").build();
        FoodOrderResponse firstResponse = mock(FoodOrderResponse.class);
        FoodOrderResponse secondResponse = mock(FoodOrderResponse.class);
        when(orders.findAll(any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            assertEquals(2, pageable.getPageNumber());
            assertEquals(25, pageable.getPageSize());
            assertEquals("createdAt: DESC,id: DESC", pageable.getSort().toString());
            return new PageImpl<>(List.of(first, second), pageable, 52);
        });
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(eq(first), nullable(String.class), any(), nullable(String.class), anyList()))
                .thenReturn(firstResponse);
        when(mapper.toResponse(eq(second), nullable(String.class), any(), nullable(String.class), anyList()))
                .thenReturn(secondResponse);

        PageResponse<FoodOrderResponse> result = service.listAll(Pageable.ofSize(25).withPage(2));

        assertEquals(List.of(firstResponse, secondResponse), result.content());
        assertEquals(2, result.page());
        assertEquals(25, result.size());
        assertEquals(52, result.totalElements());
        verify(orders).findAll(any(Pageable.class));
    }
}
