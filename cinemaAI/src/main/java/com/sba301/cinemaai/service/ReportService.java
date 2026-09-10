package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.response.report.AbandonedRateResponse;
import com.sba301.cinemaai.dto.response.report.ConcessionSalesResponse;
import com.sba301.cinemaai.dto.response.report.ExpiredUserResponse;
import com.sba301.cinemaai.dto.response.report.NoShowReportResponse;
import com.sba301.cinemaai.dto.response.report.PeakHourResponse;
import com.sba301.cinemaai.dto.response.report.RevenueReportResponse;
import com.sba301.cinemaai.dto.response.report.RoomOccupancyResponse;
import com.sba301.cinemaai.dto.response.report.ShowtimeFillResponse;
import com.sba301.cinemaai.dto.response.report.TopMovieResponse;
import com.sba301.cinemaai.dto.response.report.TopSeatResponse;
import com.sba301.cinemaai.dto.response.report.ShowtimeIncidentReportResponse;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    RevenueReportResponse getRevenue(LocalDate from, LocalDate to);

    List<TopMovieResponse> getTopMovies(LocalDate from, LocalDate to, int limit);

    List<RoomOccupancyResponse> getRoomOccupancy(LocalDate from, LocalDate to);

    List<com.sba301.cinemaai.dto.response.report.DailyOccupancyResponse> getDailyOccupancy(LocalDate from, LocalDate to);

    List<ShowtimeFillResponse> getShowtimeFill(LocalDate date, int days);

    NoShowReportResponse getNoShows(LocalDate from, LocalDate to);

    List<PeakHourResponse> getPeakHours(LocalDate from, LocalDate to);

    List<TopSeatResponse> getTopSeats(LocalDate from, LocalDate to, Long roomId);

    AbandonedRateResponse getAbandonedRate(LocalDate from, LocalDate to);

    ConcessionSalesResponse getConcessionSales(LocalDate from, LocalDate to);

    List<ExpiredUserResponse> getExpiredUsers(LocalDate from, LocalDate to);

    ShowtimeIncidentReportResponse getShowtimeIncidents(LocalDate from, LocalDate to, Long showtimeId);
}
