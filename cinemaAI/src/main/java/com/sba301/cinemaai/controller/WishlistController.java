package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.request.wishlist.WishlistCreateRequest;
import com.sba301.cinemaai.dto.response.wishlist.WishlistResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Manage the current user's movie wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add movie to wishlist", description = "Adds a movie to the authenticated user's wishlist")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Movie added to wishlist"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Movie already in wishlist"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    public ApiResponse<WishlistResponse> add(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody WishlistCreateRequest request
    ) {
        return ApiResponse.success(
                wishlistService.addToWishlist(currentUser.getUsername(), request),
                "Movie added to wishlist"
        );
    }

    @GetMapping
    @Operation(summary = "Get my wishlist", description = "Returns all movies in the authenticated user's wishlist")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wishlist returned")
    })
    public ApiResponse<List<WishlistResponse>> getMyWishlist(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(wishlistService.getWishlist(currentUser.getUsername()));
    }

    @DeleteMapping("/{movieId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove movie from wishlist", description = "Removes a specific movie from the authenticated user's wishlist")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Movie removed from wishlist"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found in wishlist")
    })
    public void remove(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "ID of the movie to remove", example = "1")
            @PathVariable Long movieId
    ) {
        wishlistService.removeFromWishlist(currentUser.getUsername(), movieId);
    }
}
