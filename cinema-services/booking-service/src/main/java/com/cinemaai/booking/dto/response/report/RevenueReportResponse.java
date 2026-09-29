package com.cinemaai.booking.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RevenueReportResponse {

    private LocalDate from;
    private LocalDate to;
    private BigDecimal totalRevenue;
    private long totalTransactions;
    private long totalTicketsSold;
    private List<ProviderRevenueResponse> byProvider; // phÃ¢n rÃ£ theo phÆ°Æ¡ng thá»©c thanh toÃ¡n

    public RevenueReportResponse(LocalDate from, LocalDate to, BigDecimal totalRevenue,
            long totalTransactions, long totalTicketsSold) {
        this(from, to, totalRevenue, totalTransactions, totalTicketsSold, List.of());
    }
}

