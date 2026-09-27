package com.cinemaai.catalog.service.impl;


import com.cinemaai.catalog.service.CinemaService;
import com.cinemaai.catalog.dto.request.cinema.CinemaRequest;
import com.cinemaai.catalog.dto.response.cinema.CinemaResponse;
import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.enums.AuditActionType;
import com.cinemaai.catalog.enums.CinemaStatus;
import com.cinemaai.catalog.exception.ConflictException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.mapper.CinemaMapper;
import com.cinemaai.catalog.repository.CinemaRepository;
import com.cinemaai.catalog.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CinemaMapper cinemaMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public CinemaResponse getPublicCinema() {
        return cinemaRepository.findFirstByStatus(CinemaStatus.ACTIVE)
                .map(cinemaMapper::toCinemaResponse)
                .orElseThrow(() -> new NotFoundException("No active cinema found"));
    }

    @Transactional(readOnly = true)
    public CinemaResponse getAdminCinema() {
        return cinemaMapper.toCinemaResponse(findSingleton());
    }

    @Transactional(readOnly = true)
    public CinemaResponse getCinema(Long id) {
        return cinemaMapper.toCinemaResponse(findById(id));
    }

    @Transactional
    public CinemaResponse update(CinemaRequest request) {
        return update(findSingleton().getId(), request);
    }

    @Transactional
    public CinemaResponse update(Long id, CinemaRequest request) {
        Cinema cinema = findById(id);
        cinemaRepository.findByName(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Cinema name already exists");
                });
        cinema.setName(request.name());
        cinema.setAddress(request.address());
        cinema.setCity(request.city());
        cinema.setPhone(request.phone());
        cinema.setStatus(request.status() == null ? cinema.getStatus() : request.status());
        auditLogService.record(AuditActionType.UPDATE, "CINEMA", cinema.getId(), cinema.getName());
        return cinemaMapper.toCinemaResponse(cinema);
    }

    @Transactional
    public CinemaResponse updateStatus(CinemaStatus status) {
        return updateStatus(findSingleton().getId(), status);
    }

    @Transactional
    public CinemaResponse updateStatus(Long id, CinemaStatus status) {
        Cinema cinema = findById(id);
        cinema.setStatus(status);
        auditLogService.record(AuditActionType.UPDATE, "CINEMA", cinema.getId(), cinema.getName() + " -> " + status);
        return cinemaMapper.toCinemaResponse(cinema);
    }

    public Cinema findById(Long id) {
        return cinemaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cinema not found"));
    }

    public Cinema findSingleton() {
        return cinemaRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NotFoundException("No cinema configured"));
    }

    public Cinema findSingletonById(Long id) {
        return findById(id);
    }
}
