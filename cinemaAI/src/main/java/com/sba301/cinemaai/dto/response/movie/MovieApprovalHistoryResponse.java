package com.sba301.cinemaai.dto.response.movie;

import com.sba301.cinemaai.enums.ApprovalAction;
import java.time.LocalDateTime;

public record MovieApprovalHistoryResponse(
        Long id,
        Long movieId,
        ApprovalAction action,
        String fromStatus,
        String toStatus,
        String comment,
        Long actorUserId,
        String actorName,
        String actorEmail,
        LocalDateTime createdAt
) {
}
