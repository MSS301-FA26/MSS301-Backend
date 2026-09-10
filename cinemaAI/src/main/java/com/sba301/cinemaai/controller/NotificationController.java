package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.notification.NotificationCreateRequest;
import com.sba301.cinemaai.dto.response.notification.NotificationResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Notification management for users and admins")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create notification for a user",
            description = "ADMIN only. Sends a notification to the specified user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Notification created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Target user not found")
    })
    public ApiResponse<NotificationResponse> create(
            @Valid @RequestBody NotificationCreateRequest request
    ) {
        return ApiResponse.success(
                notificationService.createForUser(request),
                "Notification created successfully"
        );
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get my notifications",
            description = "Returns all notifications for the authenticated user, newest first."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResponse<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(notificationService.getMyNotifications(currentUser.id()));
    }

    @GetMapping("/me/unread")
    @Operation(
            summary = "Get my unread notifications",
            description = "Returns only unread notifications for the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unread notifications returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResponse<List<NotificationResponse>> getMyUnread(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(notificationService.getMyUnread(currentUser.id()));
    }

    @PatchMapping("/{id}/read")
    @Operation(
            summary = "Mark notification as read",
            description = "Marks the specified notification as read. Returns current state if already read."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found or does not belong to current user")
    })
    public ApiResponse<NotificationResponse> markRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Notification ID") @PathVariable Long id
    ) {
        return ApiResponse.success(
                notificationService.markRead(currentUser.id(), id),
                "Notification marked as read"
        );
    }

    @PatchMapping("/me/read-all")
    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all unread notifications for the authenticated user as read."
    )
    public ApiResponse<Integer> markAllRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        int updated = notificationService.markAllRead(currentUser.id());
        return ApiResponse.success(updated, updated + " notification(s) marked as read");
    }
}
