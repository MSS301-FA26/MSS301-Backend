package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.request.ticket.TicketComboRequest;
import com.cinemaai.catalog.dto.response.ticket.TicketComboResponse;
import com.cinemaai.catalog.dto.request.ticket.TicketPricingRuleRequest;
import com.cinemaai.catalog.dto.response.ticket.TicketPricingRuleResponse;
import com.cinemaai.catalog.enums.RoomType;
import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import com.cinemaai.catalog.entity.TicketPricingRule;
import com.cinemaai.catalog.exception.ForbiddenException;
import com.cinemaai.catalog.security.AuthenticatedUser;
import com.cinemaai.catalog.security.CinemaSecurityService;
import com.cinemaai.catalog.service.TicketPricingService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final CinemaSecurityService cinemaSecurityService;

    @GetMapping("/rules")
    public ApiResponse<PageResponse<TicketPricingRuleResponse>> getRules(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) TicketType ticketType,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) SeatType seatType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(user, cinemaId);
        return ApiResponse.success(ticketPricingService.searchRules(enforcedCinemaId, ticketType, roomType, seatType, active, page, size));
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TicketPricingRuleResponse> createRule(
            @Valid @RequestBody TicketPricingRuleRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long targetCinemaId = request.cinemaId();
        if (user != null && user.isManager() && !user.isAdmin()) {
            Long userCinemaId = user.cinemaId();
            if (userCinemaId == null) {
                throw new ForbiddenException("Tài khoản Quản lý chưa được phân công cụm rạp cụ thể.");
            }
            if (targetCinemaId != null && !userCinemaId.equals(targetCinemaId)) {
                throw new ForbiddenException("Quản lý không có quyền tạo bảng giá cho rạp khác.");
            }
            targetCinemaId = userCinemaId;
        }
        TicketPricingRuleRequest scopedRequest = new TicketPricingRuleRequest(
                targetCinemaId,
                request.ticketType(),
                request.roomType(),
                request.seatType(),
                request.weekend(),
                request.holiday(),
                request.price(),
                request.active(),
                request.effectiveFrom(),
                request.effectiveTo()
        );
        return ApiResponse.success(ticketPricingService.createRule(scopedRequest), "Ticket pricing rule created successfully");
    }

    @PutMapping("/rules/{ruleId}")
    public ApiResponse<TicketPricingRuleResponse> updateRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody TicketPricingRuleRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        TicketPricingRule rule = ticketPricingService.findRule(ruleId);
        if (user != null && user.isManager() && !user.isAdmin()) {
            Long userCinemaId = user.cinemaId();
            if (rule.getCinemaId() == null) {
                throw new ForbiddenException("Quản lý không có quyền sửa đổi bảng giá mặc định toàn hệ thống (Global Pricing).");
            }
            if (!userCinemaId.equals(rule.getCinemaId())) {
                throw new ForbiddenException("Quản lý không có quyền sửa đổi bảng giá của rạp khác.");
            }
            if (request.cinemaId() != null && !userCinemaId.equals(request.cinemaId())) {
                throw new ForbiddenException("Quản lý không có quyền chuyển bảng giá sang rạp khác.");
            }
        }
        return ApiResponse.success(ticketPricingService.updateRule(ruleId, request), "Ticket pricing rule updated successfully");
    }

    @DeleteMapping("/rules/{ruleId}")
    public ApiResponse<Void> deleteRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        TicketPricingRule rule = ticketPricingService.findRule(ruleId);
        if (user != null && user.isManager() && !user.isAdmin()) {
            Long userCinemaId = user.cinemaId();
            if (rule.getCinemaId() == null) {
                throw new ForbiddenException("Quản lý không có quyền xóa bảng giá mặc định toàn hệ thống (Global Pricing).");
            }
            if (!userCinemaId.equals(rule.getCinemaId())) {
                throw new ForbiddenException("Quản lý không có quyền xóa bảng giá của rạp khác.");
            }
        }
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
