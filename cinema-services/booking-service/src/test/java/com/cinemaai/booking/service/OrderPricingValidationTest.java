package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.client.dto.CatalogProductQuoteDto;
import com.cinemaai.booking.dto.request.BookingProductRequest;
import com.cinemaai.booking.dto.request.OrderRequest;
import com.cinemaai.booking.dto.response.OrderResponse;
import com.cinemaai.booking.entity.Order;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.mapper.OrderMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.OrderRepository;
import com.cinemaai.booking.service.impl.OrderServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderPricingValidationTest {
    private OrderRepository orders;
    private CatalogClient catalog;
    private OrderMapper mapper;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        catalog = mock(CatalogClient.class);
        mapper = mock(OrderMapper.class);
        service = new OrderServiceImpl(orders, mock(BookingRepository.class), catalog,
                mock(PaymentClient.class), mapper);
    }

    @Test
    void catalogFailureFailsClosedWithoutSavingFallbackPrice() {
        var request = new OrderRequest(List.of(new BookingProductRequest(999L, null, null, false, 1)));
        when(catalog.getFoodQuote(any())).thenReturn(null);

        assertThrows(BadRequestException.class, () -> service.createStandalone(1L, request));
        verify(orders, never()).save(any());
    }

    @Test
    void rejectsInvalidQuantityAndConflictingIdentifiersBeforeCatalogCall() {
        assertThrows(BadRequestException.class, () -> service.createStandalone(1L,
                new OrderRequest(List.of(new BookingProductRequest(1L, null, null, false, 0)))));
        assertThrows(BadRequestException.class, () -> service.createStandalone(1L,
                new OrderRequest(List.of(new BookingProductRequest(1L, null, 2L, false, 1)))));
        verifyNoInteractions(catalog, orders);
    }

    @Test
    void persistsOnlyCatalogAuthoritativePriceAndTotal() {
        var request = new OrderRequest(List.of(new BookingProductRequest(50L, null, null, false, 2)));
        var snapshot = new CatalogProductQuoteDto.Response.ItemSnapshot(50L, false, "Popcorn",
                new BigDecimal("70000"), 2, new BigDecimal("140000"));
        when(catalog.getFoodQuote(any())).thenReturn(
                new CatalogProductQuoteDto.Response(List.of(snapshot), new BigDecimal("140000")));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), isNull(), any(), isNull(), anyList())).thenReturn(mock(OrderResponse.class));

        service.createStandalone(1L, request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(captor.capture());
        assertEquals(new BigDecimal("140000"), captor.getValue().getTotalAmount());
        assertEquals(new BigDecimal("70000"), captor.getValue().getItems().getFirst().getUnitPrice());
    }

    @Test
    void rejectsCatalogQuoteForDifferentProductWithoutSaving() {
        var request = new OrderRequest(List.of(new BookingProductRequest(50L, null, null, false, 2)));
        var wrongProduct = new CatalogProductQuoteDto.Response.ItemSnapshot(51L, false, "Other",
                new BigDecimal("70000"), 2, new BigDecimal("140000"));
        when(catalog.getFoodQuote(any())).thenReturn(
                new CatalogProductQuoteDto.Response(List.of(wrongProduct), new BigDecimal("140000")));

        assertThrows(BadRequestException.class, () -> service.createStandalone(1L, request));
        verify(orders, never()).save(any());
    }
}
