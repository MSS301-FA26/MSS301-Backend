package com.cinemaai.payment.dto.response;

import java.time.LocalDateTime;

public record LoyaltyReportResponse(
        LocalDateTime from,
        LocalDateTime to,
        long newMembers,
        long totalIssuedPoints,
        long totalBurnedPoints,
        double pointFlowRatio
) {}
