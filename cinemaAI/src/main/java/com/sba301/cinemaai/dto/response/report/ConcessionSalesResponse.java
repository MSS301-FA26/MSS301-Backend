package com.sba301.cinemaai.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ConcessionSalesResponse(
        LocalDate from,
        LocalDate to,
        long totalOrders,
        long totalItemsSold,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue,
        List<DailyLine> daily,
        List<Line> lines,
        List<SourceLine> sources,
        long lowStockCount,
        long outOfStockCount,
        BigDecimal grossProfit,
        Double profitMargin
) {
    public ConcessionSalesResponse(
            LocalDate from,
            LocalDate to,
            long totalOrders,
            long totalItemsSold,
            BigDecimal totalRevenue,
            BigDecimal averageOrderValue,
            List<DailyLine> daily,
            List<Line> lines,
            List<SourceLine> sources
    ) {
        this(from, to, totalOrders, totalItemsSold, totalRevenue, averageOrderValue, daily, lines, sources, 0, 0, BigDecimal.ZERO, 0.0);
    }

    public record Line(String name, long quantity, BigDecimal revenue) {
    }

    public record DailyLine(
            LocalDate date,
            long orderCount,
            long quantity,
            BigDecimal revenue
    ) {
    }

    public record SourceLine(
            String source,
            long orderCount,
            long quantity,
            BigDecimal revenue
    ) {
    }
}
