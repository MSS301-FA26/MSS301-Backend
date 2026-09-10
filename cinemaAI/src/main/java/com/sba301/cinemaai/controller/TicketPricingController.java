package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.ticket.TicketComboResponse;
import com.sba301.cinemaai.dto.request.ticket.TicketPriceValidationRequest;
import com.sba301.cinemaai.dto.response.ticket.TicketPriceValidationResponse;
import com.sba301.cinemaai.service.TicketPricingService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ticket-pricing")
@Tag(name = "Ticket Pricing")
@RequiredArgsConstructor
public class TicketPricingController {

    private final TicketPricingService ticketPricingService;

    @Hidden
    @GetMapping("/combos")
    public ApiResponse<List<TicketComboResponse>> getActiveCombos() {
        return ApiResponse.success(ticketPricingService.getCombos(true));
    }

    @PostMapping("/validate")
    public ApiResponse<TicketPriceValidationResponse> validatePrice(
            @Valid @RequestBody TicketPriceValidationRequest request
    ) {
        return ApiResponse.success(ticketPricingService.validatePrice(request), "Ticket price validated successfully");
    }
}
