package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.cinema.CinemaRequest;
import com.sba301.cinemaai.dto.response.cinema.CinemaResponse;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.enums.CinemaStatus;

public interface CinemaService {

        public CinemaResponse getPublicCinema();

        public CinemaResponse getAdminCinema();

        public CinemaResponse getCinema(Long id);

        public CinemaResponse update(CinemaRequest request);

        public CinemaResponse update(Long id, CinemaRequest request);

        public CinemaResponse updateStatus(CinemaStatus status);

        public CinemaResponse updateStatus(Long id, CinemaStatus status);

        public Cinema findById(Long id);

        public Cinema findSingleton();

        public Cinema findSingletonById(Long id);
}
