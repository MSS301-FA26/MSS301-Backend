package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.cinema.CinemaRequest;
import com.cinemaai.catalog.dto.response.cinema.CinemaResponse;
import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.enums.CinemaStatus;

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
