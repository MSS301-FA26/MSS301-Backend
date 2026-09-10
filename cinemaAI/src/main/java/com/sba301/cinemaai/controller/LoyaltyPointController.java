package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.loyalty.LoyaltyAddRequest;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyConfigurationResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.LoyaltyPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "Loyalty points for the authenticated user")
public class LoyaltyPointController {

    private final LoyaltyPointService loyaltyPointService;

    @GetMapping("/me")
    @Operation(
            summary = "Get my loyalty points",
            description = "Returns the current user's loyalty balance. Creates a new balance record (0 pts) if this is the first time."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResponse<LoyaltyResponse> getMyPoints(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(loyaltyPointService.getMyPoints(currentUser.getUsername()));
    }

    @GetMapping("/config")
    @Operation(summary = "Get loyalty conversion configuration")
    public ApiResponse<LoyaltyConfigurationResponse> getConfiguration() {
        return ApiResponse.success(loyaltyPointService.getConfiguration());
    }

    @PostMapping("/me/redeem")
    @Operation(
            summary = "Redeem loyalty points",
            description = "Customer redeems loyalty points using the current admin-configured conversion rate."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Points redeemed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient points or invalid amount"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResponse<LoyaltyResponse> redeemMyPoints(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam int points
    ) {
        return ApiResponse.success(
                loyaltyPointService.redeemMyPoints(currentUser.getUsername(), points),
                "Points redeemed successfully"
        );
    }
}
