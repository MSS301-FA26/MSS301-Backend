package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.request.ticket.TicketComboRequest;
import com.sba301.cinemaai.dto.response.ticket.TicketComboResponse;
import com.sba301.cinemaai.dto.request.ticket.TicketPricingRuleRequest;
import com.sba301.cinemaai.dto.response.ticket.TicketPricingRuleResponse;
import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
import com.sba301.cinemaai.service.TicketPricingService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ticket-pricing")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Hidden
public class AdminTicketPricingController {

    private final TicketPricingService ticketPricingService;

    @GetMapping("/rules")
    public ApiResponse<PageResponse<TicketPricingRuleResponse>> getRules(
            @RequestParam(required = false) TicketType ticketType,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) SeatType seatType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(ticketPricingService.searchRules(ticketType, roomType, seatType, active, page, size));
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TicketPricingRuleResponse> createRule(@Valid @RequestBody TicketPricingRuleRequest request) {
        return ApiResponse.success(ticketPricingService.createRule(request), "Ticket pricing rule created successfully");
    }

    @PutMapping("/rules/{ruleId}")
    public ApiResponse<TicketPricingRuleResponse> updateRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody TicketPricingRuleRequest request
    ) {
        return ApiResponse.success(ticketPricingService.updateRule(ruleId, request), "Ticket pricing rule updated successfully");
    }

    @DeleteMapping("/rules/{ruleId}")
    public ApiResponse<Void> deleteRule(@PathVariable Long ruleId) {
        ticketPricingService.deleteRule(ruleId);
        return ApiResponse.success(null, "Ticket pricing rule deleted successfully");
    }

    @GetMapping("/combos")
    public ApiResponse<PageResponse<TicketComboResponse>> getCombos(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(ticketPricingService.searchCombos(active, keyword, page, size));
    }

    @PostMapping("/combos")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TicketComboResponse> createCombo(@Valid @RequestBody TicketComboRequest request) {
        return ApiResponse.success(ticketPricingService.createCombo(request), "Ticket combo created successfully");
    }

    @PutMapping("/combos/{comboId}")
    public ApiResponse<TicketComboResponse> updateCombo(
            @PathVariable Long comboId,
            @Valid @RequestBody TicketComboRequest request
    ) {
        return ApiResponse.success(ticketPricingService.updateCombo(comboId, request), "Ticket combo updated successfully");
    }

    @DeleteMapping("/combos/{comboId}")
    public ApiResponse<Void> deleteCombo(@PathVariable Long comboId) {
        ticketPricingService.deleteCombo(comboId);
        return ApiResponse.success(null, "Ticket combo deleted successfully");
    }
}
