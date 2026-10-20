package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.catalog.CheckoutQuoteRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.catalog.CheckoutQuoteResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.CatalogCheckoutService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalog & Checkout Quote")
@RequiredArgsConstructor
public class CatalogCheckoutController {

    private final CatalogCheckoutService catalogCheckoutService;

    @PostMapping("/checkout-quote")
    public ApiResponse<CheckoutQuoteResponse> getCheckoutQuote(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckoutQuoteRequest request
    ) {
        String email = user != null ? user.getUsername() : null;
        return ApiResponse.success(catalogCheckoutService.getCheckoutQuote(email, request));
    }
}
