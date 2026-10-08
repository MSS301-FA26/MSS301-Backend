package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.movie.MovieCreateRequest;
import com.sba301.cinemaai.dto.request.movie.MovieRejectRequest;
import com.sba301.cinemaai.dto.request.movie.MovieStatusUpdateRequest;
import com.sba301.cinemaai.dto.request.movie.MovieUpdateRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.movie.MovieApprovalHistoryResponse;
import com.sba301.cinemaai.dto.response.movie.MovieResponse;
import com.sba301.cinemaai.enums.MovieApprovalStatus;
import com.sba301.cinemaai.enums.MoviePublicationStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/admin/movies")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Movies", description = "Admin movie management and approval workflow endpoints")
public class AdminMovieController {

    private final MovieService movieService;

    @GetMapping
    @Operation(summary = "Search movies (Admin)", description = "Search all movies with lifecycle filters (Admin/Staff)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movies found successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ApiResponse<PageResponse<MovieResponse>> searchMovies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) MovieApprovalStatus approvalStatus,
            @RequestParam(required = false) MoviePublicationStatus publicationStatus,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(movieService.searchAdmin(keyword, status, approvalStatus, publicationStatus, genreId, fromDate, toDate, page, size));
    }

    @GetMapping("/{movieId}")
    @Operation(summary = "Get movie details (Admin)", description = "Get detailed information about a movie (Admin/Staff)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movie found successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    public ApiResponse<MovieResponse> getMovie(@PathVariable Long movieId) {
        return ApiResponse.success(movieService.getAdmin(movieId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new movie as DRAFT (Admin/Staff)", description = "Saves movie immediately into internal library under DRAFT status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Movie draft created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Movie title already exists")
    })
    public ApiResponse<MovieResponse> createMovie(@Valid @RequestBody MovieCreateRequest request, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.create(request, userId), "Lưu bản nháp phim thành công");
    }

    @PutMapping("/{movieId}")
    @Operation(summary = "Update movie (Admin/Staff)", description = "Update movie draft. If already approved, editing moves movie back to DRAFT for re-approval")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movie updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or movie pending approval"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Movie title already exists")
    })
    public ApiResponse<MovieResponse> updateMovie(
            @PathVariable Long movieId,
            @Valid @RequestBody MovieUpdateRequest request,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.update(movieId, request, userId), "Cập nhật phim thành công");
    }

    @PostMapping("/{movieId}/submit")
    @Operation(summary = "Submit movie for approval (Admin/Staff)", description = "Validates required fields and transitions movie to PENDING_APPROVAL")
    public ApiResponse<MovieResponse> submitForApproval(@PathVariable Long movieId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.submitForApproval(movieId, userId), "Đã gửi phim chờ duyệt thành công");
    }

    @PostMapping("/{movieId}/withdraw")
    @Operation(summary = "Withdraw approval request (Admin/Staff)", description = "Withdraws pending approval request and transitions back to DRAFT")
    public ApiResponse<MovieResponse> withdrawApproval(@PathVariable Long movieId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.withdrawApproval(movieId, userId), "Đã rút yêu cầu duyệt, phim chuyển về bản nháp");
    }

    @PostMapping("/{movieId}/approve")
    @Operation(summary = "Approve movie (Admin only)", description = "Approves pending movie, enabling showtime scheduling")
    public ApiResponse<MovieResponse> approveMovie(
            @PathVariable Long movieId,
            @RequestParam(required = false) String note,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.approveMovie(movieId, userId, note), "Đã duyệt phim thành công");
    }

    @PostMapping("/{movieId}/reject")
    @Operation(summary = "Reject movie (Admin only)", description = "Rejects pending movie with mandatory reason")
    public ApiResponse<MovieResponse> rejectMovie(
            @PathVariable Long movieId,
            @Valid @RequestBody MovieRejectRequest request,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.rejectMovie(movieId, userId, request), "Đã từ chối duyệt phim");
    }

    @PostMapping("/{movieId}/publish")
    @Operation(summary = "Publish movie to public website (Admin only)", description = "Makes approved movie visible on customer website")
    public ApiResponse<MovieResponse> publishMovie(@PathVariable Long movieId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.publishMovie(movieId, userId), "Đã xuất bản phim lên website công khai");
    }

    @PostMapping("/{movieId}/unpublish")
    @Operation(summary = "Unpublish movie (Admin only)", description = "Hides movie from public website while retaining showtimes/data")
    public ApiResponse<MovieResponse> unpublishMovie(@PathVariable Long movieId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.unpublishMovie(movieId, userId), "Đã hủy xuất bản phim khỏi website công khai");
    }

    @PostMapping("/{movieId}/archive")
    @Operation(summary = "Archive movie (Admin only)", description = "Archives movie safely without breaking existing showtime or booking history")
    public ApiResponse<MovieResponse> archiveMovie(@PathVariable Long movieId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.archiveMovie(movieId, userId), "Đã lưu trữ phim");
    }

    @PostMapping("/{movieId}/unarchive")
    @Operation(summary = "Unarchive movie (Admin only)", description = "Restores movie from archive back to unarchived status")
    public ApiResponse<MovieResponse> unarchiveMovie(@PathVariable Long movieId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(movieService.unarchiveMovie(movieId, userId), "Đã khôi phục phim từ lưu trữ");
    }

    @GetMapping("/{movieId}/approval-history")
    @Operation(summary = "Get movie approval history", description = "Retrieves chronological log of submissions, reviews, rejections and approvals")
    public ApiResponse<List<MovieApprovalHistoryResponse>> getApprovalHistory(@PathVariable Long movieId) {
        return ApiResponse.success(movieService.getApprovalHistory(movieId), "Lấy lịch sử duyệt phim thành công");
    }

    @PatchMapping("/{movieId}/status")
    @Operation(summary = "Update movie status (Admin)", description = "Update the status of a movie (Admin only)")
    public ApiResponse<MovieResponse> updateStatus(
            @PathVariable Long movieId,
            @Valid @RequestBody MovieStatusUpdateRequest request
    ) {
        return ApiResponse.success(movieService.updateStatus(movieId, request), "Cập nhật trạng thái phim thành công");
    }

    @DeleteMapping("/{movieId}")
    @Operation(summary = "Delete movie (Admin)", description = "Deletes draft movie if no showtime exists; otherwise prompts archiving")
    public ApiResponse<Void> deleteMovie(@PathVariable Long movieId) {
        movieService.delete(movieId);
        return ApiResponse.success(null, "Đã xóa bản nháp phim thành công");
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.id();
        }
        return null;
    }
}
