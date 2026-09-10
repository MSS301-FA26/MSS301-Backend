package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.cinema.BulkShowtimeRequest;
import com.sba301.cinemaai.dto.request.cinema.ShowtimeRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.refund.BulkRefundResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeSeatMapResponse;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import java.time.LocalDate;
import java.util.List;

public interface ShowtimeService {

    PageResponse<ShowtimeResponse> searchPublic(Long movieId, Long roomId, LocalDate date, int page, int size);

    PageResponse<ShowtimeResponse> searchAdmin(Long movieId, Long roomId, Long cinemaId,
                                               ShowtimeStatus status, LocalDate date, int page, int size);

    ShowtimeResponse getAdmin(Long id);

    ShowtimeResponse get(Long id);

    ShowtimeResponse create(ShowtimeRequest request);

    List<com.sba301.cinemaai.dto.response.cinema.AvailableSlotResponse> getAvailableSlots(Long roomId, Long movieId, LocalDate date);

    List<ShowtimeResponse> createBulk(BulkShowtimeRequest request);

    ShowtimeResponse update(Long id, ShowtimeRequest request);

    ShowtimeResponse updateStatus(Long id, ShowtimeStatus status);

    void delete(Long id);

    ShowtimeSeatMapResponse getSeatMap(Long showtimeId);

    ShowtimeResponse cancelShowtime(Long id, String reason);

    BulkRefundResponse cancelShowtimeAndRefund(Long id, String reason);
}
