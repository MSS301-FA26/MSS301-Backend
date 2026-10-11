package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.cinema.CinemaRequest;
import com.cinemaai.catalog.dto.response.cinema.CinemaResponse;
import com.cinemaai.catalog.dto.request.cinema.DaySurchargeRequest;
import com.cinemaai.catalog.dto.response.cinema.DaySurchargeResponse;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.enums.CinemaStatus;
import com.cinemaai.catalog.exception.ForbiddenException;
import com.cinemaai.catalog.security.AuthenticatedUser;
import com.cinemaai.catalog.security.CinemaSecurityService;
import com.cinemaai.catalog.service.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/cinemas", "/api/v1/admin/cinemas/"})
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Cinemas", description = "Admin multi-cinema management endpoints - requires ADMIN role")
public class AdminCinemasController {

    private final CinemaService cinemaService;
    private final CinemaSecurityService cinemaSecurityService;

    @GetMapping({"", "/"})
    @Operation(summary = "Get all cinemas (Admin) or assigned cinema (Manager)", description = "Get list of cinemas in the system. Manager only sees their assigned cinema.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinemas retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN or MANAGER role")
    })
    public ApiResponse<List<CinemaResponse>> getCinemas(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user != null && user.isManager() && !user.isAdmin()) {
            Long cinemaId = user.cinemaId();
            if (cinemaId == null) {
                throw new ForbiddenException("Tài khoản Quản lý chưa được phân công cụm rạp cụ thể.");
            }
            return ApiResponse.success(List.of(cinemaService.getCinema(cinemaId)));
        }
        return ApiResponse.success(cinemaService.getCinemas());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"", "/"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create cinema (Admin)", description = "Create a new cinema complex (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Cinema created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cinema name already exists")
    })
    public ApiResponse<CinemaResponse> createCinema(@Valid @RequestBody CinemaRequest request) {
        return ApiResponse.success(cinemaService.create(request), "Cinema created successfully");
    }

    @GetMapping("/{cinemaId}")
    @Operation(summary = "Get cinema by ID (Admin / Manager of that cinema)", description = "Get cinema details by ID (Admin or Manager assigned to this cinema)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission for this cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<CinemaResponse> getCinema(
            @PathVariable Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        cinemaSecurityService.validateCinemaAccess(user, cinemaId);
        return ApiResponse.success(cinemaService.getCinema(cinemaId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{cinemaId}")
    @Operation(summary = "Update cinema by ID (Admin)", description = "Update cinema details by ID (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cinema name already exists")
    })
    public ApiResponse<CinemaResponse> updateCinema(@PathVariable Long cinemaId, @Valid @RequestBody CinemaRequest request) {
        return ApiResponse.success(cinemaService.update(cinemaId, request), "Cinema updated successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{cinemaId}/status")
    @Operation(summary = "Update cinema status by ID (Admin)", description = "Update status of cinema by ID (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<CinemaResponse> updateStatus(@PathVariable Long cinemaId, @RequestParam CinemaStatus status) {
        return ApiResponse.success(cinemaService.updateStatus(cinemaId, status), "Cinema status updated successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{cinemaId}")
    @Operation(summary = "Delete or deactivate cinema (Admin)", description = "Delete cinema if empty, or deactivate if has rooms (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cinema deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<Void> deleteCinema(@PathVariable Long cinemaId) {
        cinemaService.delete(cinemaId);
        return ApiResponse.success(null, "Cinema deleted successfully");
    }

    // -------------------------------------------------------------------------
    // AUDIENCE PRICE MANAGEMENT
    // -------------------------------------------------------------------------

    @GetMapping("/{cinemaId}/audience-prices")
    @Operation(
            summary = "Get audience price surcharges for a cinema (Admin / Manager)",
            description = "Returns up to 3 rows (CHILD, STUDENT, ADULT) with the additional price per audience type for this cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audience prices retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<List<com.cinemaai.catalog.dto.response.cinema.AudiencePriceResponse>> getAudiencePrices(
            @PathVariable Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        cinemaSecurityService.validateCinemaAccess(user, cinemaId);
        return ApiResponse.success(cinemaService.getAudiencePrices(cinemaId));
    }

    @PutMapping("/{cinemaId}/audience-prices")
    @Operation(
            summary = "Upsert audience price surcharge for a cinema (Admin / Manager)",
            description = "Creates or updates the additional price for a single audience type at this cinema. " +
                    "Send one request per audience type (CHILD, STUDENT, ADULT)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audience price upserted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for this cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<com.cinemaai.catalog.dto.response.cinema.AudiencePriceResponse> upsertAudiencePrice(
            @PathVariable Long cinemaId,
            @Valid @RequestBody com.cinemaai.catalog.dto.request.cinema.AudiencePriceRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        cinemaSecurityService.validateCinemaAccess(user, cinemaId);
        return ApiResponse.success(cinemaService.upsertAudiencePrice(cinemaId, request), "Audience price updated successfully");
    }

    // -------------------------------------------------------------------------
    // DAY SURCHARGES MANAGEMENT (Weekend & Holiday)
    // -------------------------------------------------------------------------

    @GetMapping("/{cinemaId}/day-surcharges")
    @Operation(
            summary = "Get weekend & holiday surcharges for a cinema (Admin / Manager)",
            description = "Returns the weekend and holiday surcharge amounts configured for this cinema."
    )
    public ApiResponse<DaySurchargeResponse> getDaySurcharges(
            @PathVariable Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        cinemaSecurityService.validateCinemaAccess(user, cinemaId);
        return ApiResponse.success(cinemaService.getDaySurcharges(cinemaId));
    }

    @PutMapping("/{cinemaId}/day-surcharges")
    @Operation(
            summary = "Update weekend & holiday surcharges for a cinema (Admin / Manager)",
            description = "Updates the weekend and holiday surcharge amounts configured for this cinema."
    )
    public ApiResponse<DaySurchargeResponse> updateDaySurcharges(
            @PathVariable Long cinemaId,
            @Valid @RequestBody DaySurchargeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        cinemaSecurityService.validateCinemaAccess(user, cinemaId);
        return ApiResponse.success(cinemaService.updateDaySurcharges(cinemaId, request), "Day surcharges updated successfully");
    }
}
