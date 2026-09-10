package com.sba301.cinemaai.service.impl;


import com.sba301.cinemaai.service.CinemaService;
import com.sba301.cinemaai.dto.request.cinema.CinemaRequest;
import com.sba301.cinemaai.dto.response.cinema.CinemaResponse;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.CinemaStatus;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.CinemaMapper;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.service.AuditLogService;
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
