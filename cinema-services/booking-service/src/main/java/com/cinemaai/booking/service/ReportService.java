package com.cinemaai.booking.service;

import com.cinemaai.booking.dto.response.report.*;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    RevenueReportResponse getRevenue(LocalDate from, LocalDate to);

    List<TopMovieResponse> getTopMovies(LocalDate from, LocalDate to, int limit);

    List<RoomOccupancyResponse> getRoomOccupancy(LocalDate from, LocalDate to);

    List<DailyOccupancyResponse> getDailyOccupancy(LocalDate from, LocalDate to);

    List<ShowtimeFillResponse> getShowtimeFill(LocalDate date, int days);

    NoShowReportResponse getNoShows(LocalDate from, LocalDate to);

    List<PeakHourResponse> getPeakHours(LocalDate from, LocalDate to);

    List<TopSeatResponse> getTopSeats(LocalDate from, LocalDate to, Long roomId);

    AbandonedRateResponse getAbandonedRate(LocalDate from, LocalDate to);

    ConcessionSalesResponse getConcessionSales(LocalDate from, LocalDate to);

    List<ExpiredUserResponse> getExpiredUsers(LocalDate from, LocalDate to);

    ShowtimeIncidentReportResponse getShowtimeIncidents(LocalDate from, LocalDate to);
}
