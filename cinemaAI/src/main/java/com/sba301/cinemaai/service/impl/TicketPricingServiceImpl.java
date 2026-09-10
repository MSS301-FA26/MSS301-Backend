package com.sba301.cinemaai.service.impl;


import com.sba301.cinemaai.service.TicketPricingService;
import com.sba301.cinemaai.dto.request.ticket.TicketComboRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.ticket.TicketComboResponse;
import com.sba301.cinemaai.dto.response.ticket.TicketLinePriceResponse;
import com.sba301.cinemaai.dto.request.ticket.TicketPriceValidationRequest;
import com.sba301.cinemaai.dto.response.ticket.TicketPriceValidationResponse;
import com.sba301.cinemaai.dto.request.ticket.TicketPricingRuleRequest;
import com.sba301.cinemaai.dto.response.ticket.TicketPricingRuleResponse;
import com.sba301.cinemaai.dto.request.ticket.TicketSelectionRequest;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.TicketCombo;
import com.sba301.cinemaai.entity.TicketPricingRule;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.repository.TicketComboRepository;
import com.sba301.cinemaai.repository.TicketPricingRuleRepository;
import com.sba301.cinemaai.service.AuditLogService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class TicketPricingServiceImpl implements TicketPricingService {

    private static final BigDecimal LATE_NIGHT_SURCHARGE = BigDecimal.valueOf(20_000);

    private final TicketPricingRuleRepository ticketPricingRuleRepository;
    private final TicketComboRepository ticketComboRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<TicketPricingRuleResponse> getRules() {
        return ticketPricingRuleRepository.findAll().stream()
                .map(this::toRuleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketPricingRuleResponse> searchRules(
            TicketType ticketType,
            com.sba301.cinemaai.enums.RoomType roomType,
            SeatType seatType,
            Boolean active,
            int page,
            int size
    ) {
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );
        return PageResponse.from(ticketPricingRuleRepository
                .searchAdmin(ticketType, roomType, seatType, active, pageable)
                .map(this::toRuleResponse));
    }

    @Transactional
    public TicketPricingRuleResponse createRule(TicketPricingRuleRequest request) {
        validateRulePolicy(request);
        validateUniqueActiveRule(null, request);
        TicketPricingRule rule = new TicketPricingRule(
                request.ticketType(),
                request.roomType(),
                normalizeSeatType(request.seatType()),
                request.weekend(),
                request.holiday(),
                request.price()
        );
        applyRuleFields(rule, request);
        TicketPricingRule saved = ticketPricingRuleRepository.save(rule);
        auditLogService.record(AuditActionType.CREATE, "TICKET_PRICING", saved.getId(), describeRule(saved));
        return toRuleResponse(saved);
    }

    @Transactional
    public TicketPricingRuleResponse updateRule(Long id, TicketPricingRuleRequest request) {
        TicketPricingRule rule = findRule(id);
        validateRulePolicy(request);
        validateUniqueActiveRule(id, request);
        applyRuleFields(rule, request);
        auditLogService.record(AuditActionType.UPDATE, "TICKET_PRICING", rule.getId(), describeRule(rule));
        return toRuleResponse(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        TicketPricingRule rule = findRule(id);
        rule.setActive(false);
        auditLogService.record(AuditActionType.DELETE, "TICKET_PRICING", rule.getId(), describeRule(rule));
    }

    @Transactional(readOnly = true)
    public List<TicketComboResponse> getCombos(boolean activeOnly) {
        return (activeOnly ? ticketComboRepository.findByActiveTrue() : ticketComboRepository.findAll())
                .stream()
                .map(this::toComboResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketComboResponse> searchCombos(Boolean active, String keyword, int page, int size) {
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<TicketCombo> combos;
        if (active != null && normalizedKeyword != null) {
            combos = ticketComboRepository.findByActiveAndNameContainingIgnoreCase(active, normalizedKeyword, pageable);
        } else if (active != null) {
            combos = ticketComboRepository.findByActive(active, pageable);
        } else if (normalizedKeyword != null) {
            combos = ticketComboRepository.findByNameContainingIgnoreCase(normalizedKeyword, pageable);
        } else {
            combos = ticketComboRepository.findAll(pageable);
        }
        return PageResponse.from(combos.map(this::toComboResponse));
    }

    @Transactional
    public TicketComboResponse createCombo(TicketComboRequest request) {
        validateComboCounts(request);
        ticketComboRepository.findByNameIgnoreCase(request.name()).ifPresent(combo -> {
            if (combo.isActive()) {
                throw new ConflictException("Ticket combo name already exists");
            }
            throw new ConflictException("Ticket combo already exists but is inactive. Do you want to create a new combo?");
        });
        TicketCombo combo = new TicketCombo(
                request.name().trim(),
                request.description(),
                request.adultCount(),
                request.childCount(),
                request.studentCount(),
                request.price()
        );
        applyComboFields(combo, request);
        TicketCombo saved = ticketComboRepository.save(combo);
        auditLogService.record(AuditActionType.CREATE, "TICKET_PRICING", saved.getId(), "Combo " + saved.getName());
        return toComboResponse(saved);
    }

    @Transactional
    public TicketComboResponse updateCombo(Long id, TicketComboRequest request) {
        validateComboCounts(request);
        TicketCombo combo = findCombo(id);
        ticketComboRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Ticket combo name already exists");
                });
        applyComboFields(combo, request);
        auditLogService.record(AuditActionType.UPDATE, "TICKET_PRICING", combo.getId(), "Combo " + combo.getName());
        return toComboResponse(combo);
    }

    @Transactional
    public void deleteCombo(Long id) {
        TicketCombo combo = findCombo(id);
        combo.setActive(false);
        auditLogService.record(AuditActionType.DELETE, "TICKET_PRICING", combo.getId(), "Combo " + combo.getName());
    }

    @Transactional(readOnly = true)
    public TicketPriceValidationResponse validatePrice(TicketPriceValidationRequest request) {
        Showtime showtime = showtimeRepository.findById(request.showtimeId())
                .orElseThrow(() -> new NotFoundException("Showtime not found"));
        Movie movie = showtime.getMovie();
        List<TicketLinePriceResponse> ticketLines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean eligible = true;

        for (TicketSelectionRequest ticket : request.tickets()) {
            SeatType effectiveSeatType = resolveSeatType(showtime, ticket);
            boolean seatAllowedByTicketType = allowsSeatType(ticket.ticketType(), effectiveSeatType);
            BigDecimal unitPrice = showtime.getPriceForTicketAndSeatType(ticket.ticketType(), effectiveSeatType);
            boolean ageAllowedByTicketType = ticket.ticketType().allowsAge(ticket.viewerAge());
            boolean ageAllowedByMovie = movie.getAgeRating() == null || movie.getAgeRating().allowsAge(ticket.viewerAge());
            boolean lineEligible = seatAllowedByTicketType && ageAllowedByTicketType && ageAllowedByMovie;
            String message = resolveTicketMessage(ticket, movie, seatAllowedByTicketType, ageAllowedByTicketType, ageAllowedByMovie);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(ticket.quantity()));
            subtotal = subtotal.add(lineTotal);
            eligible = eligible && lineEligible;
            ticketLines.add(new TicketLinePriceResponse(
                    ticket.ticketType(),
                    ticket.viewerAge(),
                    ticket.quantity(),
                    unitPrice,
                    lineTotal,
                    lineEligible,
                    message
            ));
        }

        BigDecimal comboPrice = BigDecimal.ZERO;
        BigDecimal finalAmount = subtotal;
        if (request.comboId() != null) {
            warnings.add("Ticket combo pricing is disabled. Use food combos from the food API only.");
        }

        return new TicketPriceValidationResponse(
                showtime.getId(),
                movie.getId(),
                movie.getTitle(),
                movie.getAgeRating() == null ? null : movie.getAgeRating().getLabel(),
                eligible,
                subtotal,
                comboPrice,
                finalAmount,
                ticketLines,
                warnings
        );
    }

    private SeatType normalizeSeatType(SeatType seatType) {
        return seatType == SeatType.NORMAL ? SeatType.STANDARD : seatType;
    }

    private void applyRuleFields(TicketPricingRule rule, TicketPricingRuleRequest request) {
        rule.setTicketType(request.ticketType());
        rule.setRoomType(request.roomType());
        rule.setSeatType(normalizeSeatType(request.seatType()) == null ? SeatType.STANDARD : normalizeSeatType(request.seatType()));
        rule.setWeekend(request.weekend());
        rule.setHoliday(request.holiday());
        rule.setPrice(request.price());
        rule.setActive(request.active() == null || request.active());
    }

    private void applyComboFields(TicketCombo combo, TicketComboRequest request) {
        combo.setName(request.name().trim());
        combo.setDescription(request.description());
        combo.setAdultCount(request.adultCount());
        combo.setChildCount(request.childCount());
        combo.setStudentCount(request.studentCount());
        combo.setPrice(request.price());
        combo.setActive(request.active() == null || request.active());
    }

    private SeatType resolveSeatType(Showtime showtime, TicketSelectionRequest ticket) {
        if (ticket.seatId() == null) {
            return normalizeSeatType(ticket.seatType());
        }
        Seat seat = seatRepository.findById(ticket.seatId())
                .orElseThrow(() -> new NotFoundException("Seat not found"));
        if (!seat.getRoom().getId().equals(showtime.getRoom().getId())) {
            throw new BadRequestException("Seat does not belong to showtime room");
        }
        return normalizeSeatType(seat.getSeatType());
    }

    private boolean allowsSeatType(TicketType ticketType, SeatType seatType) {
        return true;
    }

    private boolean matchesCombo(TicketCombo combo, List<TicketSelectionRequest> tickets) {
        Map<TicketType, Integer> counts = new EnumMap<>(TicketType.class);
        for (TicketSelectionRequest ticket : tickets) {
            counts.merge(ticket.ticketType(), ticket.quantity(), Integer::sum);
        }
        return counts.getOrDefault(TicketType.ADULT, 0) == combo.getAdultCount()
                && counts.getOrDefault(TicketType.CHILD, 0) == combo.getChildCount()
                && counts.getOrDefault(TicketType.STUDENT, 0) == combo.getStudentCount();
    }

    private String resolveTicketMessage(
            TicketSelectionRequest ticket,
            Movie movie,
            boolean seatAllowedByTicketType,
            boolean ageAllowedByTicketType,
            boolean ageAllowedByMovie
    ) {
        if (!seatAllowedByTicketType) {
            return "Couple seat only supports adult ticket";
        }
        if (!ageAllowedByTicketType) {
            return "Viewer age is not valid for ticket type " + ticket.ticketType();
        }
        if (!ageAllowedByMovie) {
            return "Viewer age does not meet movie age rating " + movie.getAgeRating().getLabel();
        }
        return "Eligible";
    }

    private void validateComboCounts(TicketComboRequest request) {
        int total = request.adultCount() + request.childCount() + request.studentCount();
        if (total <= 0) {
            throw new BadRequestException("Ticket combo must contain at least one ticket");
        }
    }

    private void validateRulePolicy(TicketPricingRuleRequest request) {
        // All ticket types can be priced for all seat types.
    }

    private void validateUniqueActiveRule(Long currentRuleId, TicketPricingRuleRequest request) {
        boolean requestedActive = request.active() == null || request.active();
        if (!requestedActive) {
            return;
        }
        boolean exists = currentRuleId == null
                ? ticketPricingRuleRepository.existsByTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrue(
                        request.ticketType(),
                        request.roomType(),
                        normalizeSeatType(request.seatType()),
                        request.weekend(),
                        request.holiday()
                )
                : ticketPricingRuleRepository.existsByTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrueAndIdNot(
                        request.ticketType(),
                        request.roomType(),
                        normalizeSeatType(request.seatType()),
                        request.weekend(),
                        request.holiday(),
                        currentRuleId
                );
        if (exists) {
            throw new ConflictException("Active ticket pricing rule already exists for this ticket type, room type, seat type, weekend, and holiday");
        }
    }

    private String describeRule(TicketPricingRule rule) {
        return rule.getTicketType() + "/" + rule.getRoomType() + "/" + rule.getSeatType() + " = " + rule.getPrice();
    }

    private TicketPricingRule findRule(Long id) {
        return ticketPricingRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket pricing rule not found"));
    }

    private TicketCombo findCombo(Long id) {
        return ticketComboRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket combo not found"));
    }

    private TicketPricingRuleResponse toRuleResponse(TicketPricingRule rule) {
        return new TicketPricingRuleResponse(
                rule.getId(),
                rule.getTicketType(),
                rule.getRoomType(),
                rule.getSeatType(),
                rule.isWeekend(),
                rule.isHoliday(),
                rule.getPrice(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    private TicketComboResponse toComboResponse(TicketCombo combo) {
        return new TicketComboResponse(
                combo.getId(),
                combo.getName(),
                combo.getDescription(),
                combo.getAdultCount(),
                combo.getChildCount(),
                combo.getStudentCount(),
                combo.getPrice(),
                combo.isActive(),
                combo.getCreatedAt(),
                combo.getUpdatedAt()
        );
    }
}
