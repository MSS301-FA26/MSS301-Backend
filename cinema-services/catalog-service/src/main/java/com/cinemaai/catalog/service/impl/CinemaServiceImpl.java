package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.request.cinema.AudiencePriceRequest;
import com.cinemaai.catalog.dto.request.cinema.CinemaRequest;
import com.cinemaai.catalog.dto.response.cinema.AudiencePriceResponse;
import com.cinemaai.catalog.dto.response.cinema.CinemaResponse;
import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.entity.CinemaAudiencePrice;
import com.cinemaai.catalog.enums.AudienceType;
import com.cinemaai.catalog.enums.AuditActionType;
import com.cinemaai.catalog.enums.CinemaStatus;
import com.cinemaai.catalog.exception.ConflictException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.mapper.CinemaMapper;
import com.cinemaai.catalog.enums.RoomType;
import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.entity.TicketPricingRule;
import com.cinemaai.catalog.repository.CinemaAudiencePriceRepository;
import com.cinemaai.catalog.repository.CinemaRepository;
import com.cinemaai.catalog.repository.RoomRepository;
import com.cinemaai.catalog.repository.TicketPricingRuleRepository;
import com.cinemaai.catalog.service.AuditLogService;
import com.cinemaai.catalog.service.CinemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final CinemaMapper cinemaMapper;
    private final AuditLogService auditLogService;
    private final CinemaAudiencePriceRepository audiencePriceRepository;
    private final TicketPricingRuleRepository ticketPricingRuleRepository;

    // =========================================================================
    // READ OPERATIONS
    // =========================================================================

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

    // =========================================================================
    // WRITE OPERATIONS
    // =========================================================================

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
            auditLogService.record(AuditActionType.UPDATE, "CINEMA", cinema.getId(),
                    "Deactivated cinema with " + roomCount + " rooms");
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
        auditLogService.record(AuditActionType.UPDATE, "CINEMA", cinema.getId(),
                cinema.getName() + " -> " + status);
        return cinemaMapper.toCinemaResponse(cinema);
    }

    // =========================================================================
    // ENTITY LOOKUPS
    // =========================================================================

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

    // =========================================================================
    // AUDIENCE PRICE MANAGEMENT
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AudiencePriceResponse> getAudiencePrices(Long cinemaId) {
        findById(cinemaId); // validate cinema exists
        return audiencePriceRepository.findByCinemaId(cinemaId)
                .stream()
                .map(this::toAudiencePriceResponse)
                .toList();
    }

    @Transactional
    public AudiencePriceResponse upsertAudiencePrice(Long cinemaId, AudiencePriceRequest request) {
        findById(cinemaId); // validate cinema exists
        CinemaAudiencePrice entity = audiencePriceRepository
                .findByCinemaIdAndAudienceType(cinemaId, request.audienceType())
                .orElseGet(() -> new CinemaAudiencePrice(cinemaId, request.audienceType(), BigDecimal.ZERO));
        entity.setAdditionalPrice(request.additionalPrice());
        CinemaAudiencePrice saved = audiencePriceRepository.save(entity);
        auditLogService.record(
                AuditActionType.UPDATE,
                "CINEMA_AUDIENCE_PRICE",
                cinemaId,
                request.audienceType() + " -> " + request.additionalPrice()
        );
        AudiencePriceResponse response = toAudiencePriceResponse(saved);
        syncTicketPricingRules(cinemaId, request.audienceType(), response.standardPrice(), response.vipPrice(), response.couplePrice());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<AudienceType, BigDecimal> getAudiencePriceMap(Long cinemaId) {
        return audiencePriceRepository.findByCinemaId(cinemaId)
                .stream()
                .collect(Collectors.toMap(
                        CinemaAudiencePrice::getAudienceType,
                        CinemaAudiencePrice::getAdditionalPrice
                ));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private AudiencePriceResponse toAudiencePriceResponse(CinemaAudiencePrice entity) {
        BigDecimal baseStandard = BigDecimal.valueOf(7000);
        BigDecimal baseVip = BigDecimal.valueOf(9000);
        BigDecimal baseCouple = BigDecimal.valueOf(15000);

        List<Room> cinemaRooms = roomRepository.findByCinemaId(entity.getCinemaId());
        for (Room r : cinemaRooms) {
            if (r.getStandardPrice() != null && r.getStandardPrice().signum() > 0) {
                baseStandard = r.getStandardPrice();
                if (r.getVipPrice() != null) baseVip = r.getVipPrice();
                if (r.getCouplePrice() != null) baseCouple = r.getCouplePrice();
                break;
            }
        }

        BigDecimal additional = entity.getAdditionalPrice() != null ? entity.getAdditionalPrice() : BigDecimal.ZERO;
        BigDecimal calcStandard = baseStandard.add(additional);
        BigDecimal calcVip = baseVip.add(additional);
        BigDecimal calcCouple = baseCouple.add(additional.multiply(BigDecimal.valueOf(2)));

        return new AudiencePriceResponse(
                entity.getId(),
                entity.getCinemaId(),
                entity.getAudienceType(),
                entity.getAdditionalPrice(),
                calcStandard,
                calcVip,
                calcCouple,
                entity.getUpdatedAt()
        );
    }

    private void syncTicketPricingRules(Long cinemaId, AudienceType audienceType, BigDecimal standardPrice, BigDecimal vipPrice, BigDecimal couplePrice) {
        try {
            TicketType ticketType = TicketType.valueOf(audienceType.name());
            syncSingleRule(cinemaId, ticketType, RoomType.STANDARD, SeatType.STANDARD, standardPrice);
            syncSingleRule(cinemaId, ticketType, RoomType.STANDARD, SeatType.VIP, vipPrice);
            syncSingleRule(cinemaId, ticketType, RoomType.STANDARD, SeatType.COUPLE, couplePrice);
        } catch (Exception e) {
            log.warn("Failed to sync ticket pricing rules for cinema {}: {}", cinemaId, e.getMessage());
        }
    }

    private void syncSingleRule(Long cinemaId, TicketType ticketType, RoomType roomType, SeatType seatType, BigDecimal price) {
        if (price == null || price.signum() <= 0) return;
        var existing = ticketPricingRuleRepository
                .findFirstByCinemaIdAndTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrueOrderByUpdatedAtDesc(
                        cinemaId, ticketType, roomType, seatType, false, false);
        if (existing.isPresent()) {
            TicketPricingRule rule = existing.get();
            rule.setPrice(price);
            ticketPricingRuleRepository.save(rule);
        } else {
            TicketPricingRule rule = new TicketPricingRule(cinemaId, ticketType, roomType, seatType, false, false, price);
            ticketPricingRuleRepository.save(rule);
        }
    }
}
