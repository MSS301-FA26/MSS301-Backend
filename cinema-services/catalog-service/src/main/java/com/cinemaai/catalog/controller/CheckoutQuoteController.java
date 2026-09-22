package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.quote.CheckoutQuoteRequest;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.quote.CheckoutQuoteResponse;
import com.cinemaai.catalog.service.CheckoutQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CheckoutQuoteController {
    private final CheckoutQuoteService quotes;

    @PostMapping({"/internal/v1/catalog/checkout-quote", "/api/v1/catalog/checkout-quote"})
    public ApiResponse<CheckoutQuoteResponse> quote(@Valid @RequestBody CheckoutQuoteRequest request) {
        return ApiResponse.success(quotes.quote(request), "Checkout quote calculated");
    }
}
