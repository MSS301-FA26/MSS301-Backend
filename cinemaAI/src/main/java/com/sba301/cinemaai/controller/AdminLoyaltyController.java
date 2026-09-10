package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.loyalty.LoyaltyAddRequest;
import com.sba301.cinemaai.dto.request.loyalty.LoyaltyConfigurationRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyConfigurationResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyReportResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyTransactionResponse;
import com.sba301.cinemaai.service.LoyaltyPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/loyalty")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Loyalty", description = "Admin endpoints for managing user loyalty points")
public class AdminLoyaltyController {

    private final LoyaltyPointService loyaltyPointService;

    @GetMapping("/config")
    @Operation(summary = "Get loyalty configuration")
    public ApiResponse<LoyaltyConfigurationResponse> getConfiguration() {
        return ApiResponse.success(loyaltyPointService.getConfiguration());
    }

    @PutMapping("/config")
    @Operation(summary = "Update loyalty configuration")
    public ApiResponse<LoyaltyConfigurationResponse> updateConfiguration(
            @Valid @RequestBody LoyaltyConfigurationRequest request
    ) {
        return ApiResponse.success(loyaltyPointService.updateConfiguration(request), "Loyalty configuration updated");
    }

    @GetMapping("/transactions")
    @Operation(summary = "Search loyalty point transaction audit trail")
    public ApiResponse<PageResponse<LoyaltyTransactionResponse>> searchTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(loyaltyPointService.searchTransactions(keyword, from, to, page, size));
    }

    @GetMapping("/report")
    @Operation(summary = "Get loyalty dashboard report")
    public ApiResponse<LoyaltyReportResponse> getReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ApiResponse.success(loyaltyPointService.getReport(from, to));
    }

    @PostMapping("/expire-now")
    @Operation(summary = "Manually expire all active loyalty points")
    public ApiResponse<Integer> expireNow() {
        int affectedAccounts = loyaltyPointService.expireAllActivePoints("ADMIN");
        return ApiResponse.success(affectedAccounts, "Expired points for " + affectedAccounts + " account(s)");
    }

    @PostMapping("/add")
    @Operation(
            summary = "Add points to a user",
            description = "Manually adds loyalty points to the specified user (ADMIN only)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Points added"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<LoyaltyResponse> addPoints(
            @Valid @RequestBody LoyaltyAddRequest request
    ) {
        return ApiResponse.success(
                loyaltyPointService.addPoints(request),
                "Points added successfully"
        );
    }

    @PostMapping("/{userId}/redeem")
    @Operation(
            summary = "Redeem points from a user",
            description = "Deducts redeemed loyalty points from the specified user (ADMIN only)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Points redeemed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient points or invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<LoyaltyResponse> redeemPoints(
            @Parameter(description = "Target user ID") @PathVariable Long userId,
            @Parameter(description = "Points to redeem") @RequestParam @Min(1) int points
    ) {
        return ApiResponse.success(
                loyaltyPointService.redeemPoints(userId, points),
                "Points redeemed successfully"
        );
    }
}
