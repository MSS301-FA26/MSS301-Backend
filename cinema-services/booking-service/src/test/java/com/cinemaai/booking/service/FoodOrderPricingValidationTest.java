package com.cinemaai.booking.service;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.client.dto.CatalogFoodQuoteDto;
import com.cinemaai.booking.dto.request.BookingFoodRequest;
import com.cinemaai.booking.dto.request.FoodOrderRequest;
import com.cinemaai.booking.dto.response.FoodOrderResponse;
import com.cinemaai.booking.entity.FoodOrder;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.mapper.FoodOrderMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.FoodOrderRepository;
import com.cinemaai.booking.service.impl.FoodOrderServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FoodOrderPricingValidationTest {
    private FoodOrderRepository orders;
    private CatalogClient catalog;
    private FoodOrderMapper mapper;
    private FoodOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        orders = mock(FoodOrderRepository.class);
        catalog = mock(CatalogClient.class);
        mapper = mock(FoodOrderMapper.class);
        service = new FoodOrderServiceImpl(orders, mock(BookingRepository.class), catalog,
                mock(PaymentClient.class), mapper);
    }

    @Test
    void catalogFailureFailsClosedWithoutSavingFallbackPrice() {
        var request = new FoodOrderRequest(List.of(new BookingFoodRequest(999L, null, null, false, 1)));
        when(catalog.getFoodQuote(any())).thenReturn(null);

        assertThrows(BadRequestException.class, () -> service.createStandalone(1L, request));
        verify(orders, never()).save(any());
    }

    @Test
    void rejectsInvalidQuantityAndConflictingIdentifiersBeforeCatalogCall() {
        assertThrows(BadRequestException.class, () -> service.createStandalone(1L,
                new FoodOrderRequest(List.of(new BookingFoodRequest(1L, null, null, false, 0)))));
        assertThrows(BadRequestException.class, () -> service.createStandalone(1L,
                new FoodOrderRequest(List.of(new BookingFoodRequest(1L, null, 2L, false, 1)))));
        verifyNoInteractions(catalog, orders);
    }

    @Test
    void persistsOnlyCatalogAuthoritativePriceAndTotal() {
        var request = new FoodOrderRequest(List.of(new BookingFoodRequest(50L, null, null, false, 2)));
        var snapshot = new CatalogFoodQuoteDto.Response.ItemSnapshot(50L, false, "Popcorn",
                new BigDecimal("70000"), 2, new BigDecimal("140000"));
        when(catalog.getFoodQuote(any())).thenReturn(
                new CatalogFoodQuoteDto.Response(List.of(snapshot), new BigDecimal("140000")));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), isNull(), any(), isNull(), anyList())).thenReturn(mock(FoodOrderResponse.class));

        service.createStandalone(1L, request);

        ArgumentCaptor<FoodOrder> captor = ArgumentCaptor.forClass(FoodOrder.class);
        verify(orders).save(captor.capture());
        assertEquals(new BigDecimal("140000"), captor.getValue().getTotalAmount());
        assertEquals(new BigDecimal("70000"), captor.getValue().getItems().getFirst().getUnitPrice());
    }

    @Test
    void rejectsCatalogQuoteForDifferentProductWithoutSaving() {
        var request = new FoodOrderRequest(List.of(new BookingFoodRequest(50L, null, null, false, 2)));
        var wrongProduct = new CatalogFoodQuoteDto.Response.ItemSnapshot(51L, false, "Other",
                new BigDecimal("70000"), 2, new BigDecimal("140000"));
        when(catalog.getFoodQuote(any())).thenReturn(
                new CatalogFoodQuoteDto.Response(List.of(wrongProduct), new BigDecimal("140000")));

        assertThrows(BadRequestException.class, () -> service.createStandalone(1L, request));
        verify(orders, never()).save(any());
    }
}
