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
    private final com.cinemaai.catalog.repository.RoomRepository roomRepository;
    private final CinemaMapper cinemaMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public CinemaResponse getPublicCinema() {
        return cinemaRepository.findFirstByStatus(CinemaStatus.ACTIVE)
                .map(cinemaMapper::toCinemaResponse)
                .orElseThrow(() -> new NotFoundException("No active cinema found"));
    }

    @Transactional(readOnly = true)
    public java.util.List<CinemaResponse> getPublicCinemas() {
        return cinemaRepository.findByStatusOrderByIdAsc(CinemaStatus.ACTIVE)
                .stream()
                .map(cinemaMapper::toCinemaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CinemaResponse getAdminCinema() {
        return cinemaMapper.toCinemaResponse(findSingleton());
    }

    @Transactional(readOnly = true)
    public java.util.List<CinemaResponse> getCinemas() {
        return cinemaRepository.findAllByOrderByIdAsc()
                .stream()
                .map(cinemaMapper::toCinemaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CinemaResponse getCinema(Long id) {
        return cinemaMapper.toCinemaResponse(findById(id));
    }

    @Transactional
    public CinemaResponse create(CinemaRequest request) {
        cinemaRepository.findFirstByName(request.name()).ifPresent(c -> {
            throw new ConflictException("Cinema name already exists");
        });
        Cinema cinema = new Cinema(
                request.name(),
                request.address(),
                request.city(),
                request.phone()
        );
        if (request.status() != null) {
            cinema.setStatus(request.status());
        }
        Cinema saved = cinemaRepository.save(cinema);
        auditLogService.record(AuditActionType.CREATE, "CINEMA", saved.getId(), saved.getName());
        return cinemaMapper.toCinemaResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Cinema cinema = findById(id);
        long roomCount = roomRepository.findByCinema(cinema).size();
        if (roomCount > 0) {
            cinema.setStatus(CinemaStatus.INACTIVE);
            cinemaRepository.save(cinema);
            auditLogService.record(AuditActionType.UPDATE, "CINEMA", cinema.getId(), "Deactivated cinema with " + roomCount + " rooms");
        } else {
            cinemaRepository.delete(cinema);
            auditLogService.record(AuditActionType.DELETE, "CINEMA", id, cinema.getName());
        }
    }

    @Transactional
    public CinemaResponse update(CinemaRequest request) {
        return update(findSingleton().getId(), request);
    }

    @Transactional
    public CinemaResponse update(Long id, CinemaRequest request) {
        Cinema cinema = findById(id);
        cinemaRepository.findFirstByName(request.name())
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
                .orElseGet(() -> cinemaRepository.save(
                        new Cinema("CinemaAI Central", "1 Cinema Street", "Ho Chi Minh City", "0900000000")
                ));
    }

    public Cinema findSingletonById(Long id) {
        return findById(id);
    }
}
