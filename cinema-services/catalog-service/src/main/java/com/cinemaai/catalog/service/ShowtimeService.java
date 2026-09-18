package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.cinema.BulkShowtimeRequest;
import com.cinemaai.catalog.dto.request.cinema.ShowtimeRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeSeatMapResponse;
import com.cinemaai.catalog.enums.ShowtimeStatus;
import java.time.LocalDate;
import java.util.List;

public interface ShowtimeService {

    PageResponse<ShowtimeResponse> searchPublic(Long movieId, Long roomId, LocalDate date, int page, int size);

    PageResponse<ShowtimeResponse> searchAdmin(Long movieId, Long roomId, Long cinemaId,
                                               ShowtimeStatus status, LocalDate date, int page, int size);

    ShowtimeResponse getAdmin(Long id);

    ShowtimeResponse get(Long id);

    ShowtimeResponse create(ShowtimeRequest request);

    List<com.cinemaai.catalog.dto.response.cinema.AvailableSlotResponse> getAvailableSlots(Long roomId, Long movieId, LocalDate date);

    List<ShowtimeResponse> createBulk(BulkShowtimeRequest request);

    ShowtimeResponse update(Long id, ShowtimeRequest request);

    ShowtimeResponse updateStatus(Long id, ShowtimeStatus status);

    void delete(Long id);

    ShowtimeSeatMapResponse getSeatMap(Long showtimeId);

    ShowtimeResponse cancelShowtime(Long id, String reason);

}
