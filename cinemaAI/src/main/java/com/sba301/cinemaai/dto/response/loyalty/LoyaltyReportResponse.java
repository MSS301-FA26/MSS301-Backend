package com.sba301.cinemaai.dto.response.loyalty;

import java.time.LocalDateTime;

public record LoyaltyReportResponse(
        LocalDateTime from,
        LocalDateTime to,
        long newMembers,
        long totalIssuedPoints,
        long totalBurnedPoints,
        double pointFlowRatio
) {
}
