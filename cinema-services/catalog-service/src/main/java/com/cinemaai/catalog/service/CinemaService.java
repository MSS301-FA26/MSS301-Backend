package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.cinema.AudiencePriceRequest;
import com.cinemaai.catalog.dto.request.cinema.CinemaRequest;
import com.cinemaai.catalog.dto.response.cinema.AudiencePriceResponse;
import com.cinemaai.catalog.dto.response.cinema.CinemaResponse;
import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.entity.CinemaAudiencePrice;
import com.cinemaai.catalog.enums.AudienceType;
import com.cinemaai.catalog.enums.CinemaStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CinemaService {

        public CinemaResponse getPublicCinema();

        public java.util.List<CinemaResponse> getPublicCinemas();

        public CinemaResponse getAdminCinema();

        public java.util.List<CinemaResponse> getCinemas();

        public CinemaResponse getCinema(Long id);

        public CinemaResponse create(CinemaRequest request);

        public void delete(Long id);

        public CinemaResponse update(CinemaRequest request);

        public CinemaResponse update(Long id, CinemaRequest request);

        public CinemaResponse updateStatus(CinemaStatus status);

        public CinemaResponse updateStatus(Long id, CinemaStatus status);

        public Cinema findById(Long id);

        public Cinema findSingleton();

        public Cinema findSingletonById(Long id);

        /** Return all audience price configs for the given cinema (up to 3 entries: CHILD/STUDENT/ADULT). */
        List<AudiencePriceResponse> getAudiencePrices(Long cinemaId);

        /**
         * Upsert a single audience type price for the given cinema.
         * Creates a new row if not present, updates existing row otherwise.
         */
        AudiencePriceResponse upsertAudiencePrice(Long cinemaId, AudiencePriceRequest request);

        /**
         * Convenience method used by ShowtimeServiceImpl.
         * Returns a map of AudienceType -> additionalPrice for the cinema.
         * Returns an empty map if not configured.
         */
        Map<AudienceType, BigDecimal> getAudiencePriceMap(Long cinemaId);
}
