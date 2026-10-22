package com.cinemaai.booking.service;

import com.cinemaai.booking.dto.response.report.AdminAnalyticsResponse;
import com.cinemaai.booking.dto.response.report.ConcessionSalesResponse;
import java.time.LocalDate;

public interface AdminAnalyticsService {
    AdminAnalyticsResponse getAnalytics(LocalDate from, LocalDate to, Long cinemaId);
    ConcessionSalesResponse getConcessions(LocalDate from, LocalDate to, Long cinemaId);
}
