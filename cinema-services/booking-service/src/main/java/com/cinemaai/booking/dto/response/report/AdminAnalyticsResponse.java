package com.cinemaai.booking.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminAnalyticsResponse(
        LocalDate from, LocalDate to, Long cinemaId, Summary summary, List<Daily> daily,
        List<CinemaLine> cinemas, List<TopMovieResponse> topMovies, List<StatusLine> bookingStatuses,
        List<PeakHourResponse> peakHours, List<RoomOccupancyResponse> rooms,
        List<DailyOccupancyResponse> dailyOccupancy, List<ShowtimeFillResponse> showtimes,
        NoShowReportResponse noShows, AbandonedRateResponse abandoned, ConcessionSalesResponse concessions
) {
    public record Summary(BigDecimal totalRevenue, BigDecimal ticketRevenue, BigDecimal foodRevenue,
                          long totalTransactions, long ticketsSold, long foodOrders,
                          long showtimeCount, long totalCapacity, double occupancyRate,
                          long refundedBookings, BigDecimal refundAmount) {}
    public record Daily(LocalDate date, BigDecimal ticketRevenue, BigDecimal foodRevenue,
                        BigDecimal totalRevenue, long ticketsSold, long transactions) {}
    public record CinemaLine(Long cinemaId, String cinemaName, BigDecimal ticketRevenue,
                             BigDecimal foodRevenue, BigDecimal totalRevenue, long ticketsSold,
                             long transactions, long showtimes, long capacity, double occupancyRate) {}
    public record StatusLine(String status, long count) {}
}
