package com.sba301.cinemaai.service.impl;


import com.sba301.cinemaai.dto.request.cinema.RoomRequest;
import com.sba301.cinemaai.dto.response.cinema.RoomResponse;
import com.sba301.cinemaai.dto.request.cinema.SeatLayoutRequest;
import com.sba301.cinemaai.dto.request.cinema.SeatRowGenerationRequest;
import com.sba301.cinemaai.dto.response.cinema.SeatResponse;
import com.sba301.cinemaai.dto.request.cinema.SeatUpdateRequest;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.SeatRow;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.RoomStatus;
import com.sba301.cinemaai.enums.SeatStatus;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.CinemaMapper;
import com.sba301.cinemaai.repository.RoomRepository;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.repository.SeatRowRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.CinemaService;
import com.sba301.cinemaai.service.RoomService;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final SeatRowRepository seatRowRepository;
    private final CinemaService cinemaService;
    private final CinemaMapper cinemaMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<RoomResponse> getRooms() {
        Cinema cinema = cinemaService.findSingleton();
        return roomRepository.findByCinema(cinema)
                .stream()
                .map(cinemaMapper::toRoomResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByCinema(Long cinemaId) {
        Cinema cinema = cinemaService.findSingletonById(cinemaId);
        return roomRepository.findByCinema(cinema)
                .stream()
                .map(cinemaMapper::toRoomResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoom(Long id) {
        return cinemaMapper.toRoomResponse(findById(id));
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        Cinema cinema = cinemaService.findSingleton();
        String roomName = normalizeRoomName(request.name());
        roomRepository.findByCinemaAndNameIgnoreCase(cinema, roomName).ifPresent(room -> {
            throw new ConflictException("Room name already exists in this cinema");
        });
        Room room = new Room(cinema, roomName, request.roomType(), request.rowCount(), request.columnCount());
        room.setStatus(request.status() == null ? RoomStatus.ACTIVE : request.status());
        Room saved = roomRepository.save(room);
        auditLogService.record(AuditActionType.CREATE, "ROOM", saved.getId(), saved.getName());
        return cinemaMapper.toRoomResponse(saved);
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = findById(id);
        Cinema cinema = room.getCinema();
        String roomName = normalizeRoomName(request.name());
        roomRepository.findByCinemaAndNameIgnoreCase(cinema, roomName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Room name already exists in this cinema");
                });
        room.setName(roomName);
        room.setRoomType(request.roomType());
        room.setRowCount(request.rowCount());
        room.setColumnCount(request.columnCount());
        room.setStatus(request.status() == null ? room.getStatus() : request.status());
        auditLogService.record(AuditActionType.UPDATE, "ROOM", room.getId(), room.getName());
        return cinemaMapper.toRoomResponse(room);
    }

    @Transactional
    public RoomResponse updateStatus(Long id, RoomStatus status) {
        Room room = findById(id);
        room.setStatus(status);
        auditLogService.record(AuditActionType.UPDATE, "ROOM", room.getId(), room.getName() + " -> " + status);
        return cinemaMapper.toRoomResponse(room);
    }

    @Transactional
    public List<SeatResponse> createSeats(Long roomId, SeatLayoutRequest request) {
        Room room = findById(roomId);
        List<Seat> existingSeats = seatRepository.findByRoom(room);
        if (!existingSeats.isEmpty()) {
            throw new ConflictException("Room already has seats");
        }
        validateSeatLayout(room, request);
        applySeatLayout(room, request);
        auditLogService.record(AuditActionType.CREATE, "ROOM", room.getId(), room.getName() + " - tạo sơ đồ ghế");
        return getSeats(roomId);
    }

    @Transactional
    public List<SeatResponse> replaceSeats(Long roomId, SeatLayoutRequest request) {
        Room room = findById(roomId);
        List<Seat> existingSeats = seatRepository.findByRoom(room);
        if (existingSeats.isEmpty()) {
            throw new ConflictException("Room has no seats to replace; create seats first");
        }
        validateSeatLayout(room, request);
        seatRepository.deleteAll(existingSeats);
        seatRepository.flush();
        seatRowRepository.deleteAll(seatRowRepository.findByRoom(room));
        seatRowRepository.flush();
        applySeatLayout(room, request);
        auditLogService.record(AuditActionType.UPDATE, "ROOM", room.getId(), room.getName() + " - thay sơ đồ ghế");
        return getSeats(roomId);
    }

    private void applySeatLayout(Room room, SeatLayoutRequest request) {
        if (request.rows() == null || request.rows().isEmpty()) {
            generateDefaultSeats(room, request.defaultSeatType());
        } else {
            generateCustomSeats(room, request);
        }
    }

    private void validateSeatLayout(Room room, SeatLayoutRequest request) {
        if (request.rows() == null || request.rows().isEmpty()) {
            if (request.defaultSeatType() == SeatType.COUPLE && room.getColumnCount() % 2 != 0) {
                throw new BadRequestException("Couple seats require an even number of seats in each row");
            }
            return;
        }
        if (request.rows().size() > room.getRowCount()) {
            throw new BadRequestException(
                    "Seat layout has " + request.rows().size()
                            + " rows, but room allows at most " + room.getRowCount() + " rows"
            );
        }

        Set<Integer> displayOrders = new HashSet<>();
        for (SeatRowGenerationRequest row : request.rows()) {
            SeatType rowSeatType = row.seatType() == null ? request.defaultSeatType() : row.seatType();
            if (rowSeatType == SeatType.COUPLE && row.seatNumbers().size() % 2 != 0) {
                throw new BadRequestException("Couple seat row " + normalizeRowLabel(row.rowLabel()) + " must have an even number of seats");
            }
            if (row.displayOrder() > room.getRowCount()) {
                throw new BadRequestException(
                        "Display order " + row.displayOrder()
                                + " exceeds room row count " + room.getRowCount()
                );
            }
            if (!displayOrders.add(row.displayOrder())) {
                throw new BadRequestException("Duplicate display order: " + row.displayOrder());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(Long roomId) {
        Room room = findById(roomId);
        return seatRepository.findByRoom(room)
                .stream()
                .sorted(Comparator.comparing((Seat seat) -> seat.getSeatRow().getDisplayOrder())
                        .thenComparingInt(Seat::getDisplayColumn))
                .map(cinemaMapper::toSeatResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeatResponse getSeat(Long seatId) {
        return cinemaMapper.toSeatResponse(findSeatById(seatId));
    }

    @Transactional
    public SeatResponse updateSeat(Long seatId, SeatUpdateRequest request) {
        Seat seat = findSeatById(seatId);
        if (request.seatType() == SeatType.COUPLE || seat.getSeatType() == SeatType.COUPLE) {
            Seat partner = findCouplePartner(seat);
            partner.setSeatType(request.seatType());
            partner.setStatus(request.status());
        }
        seat.setSeatType(request.seatType());
        seat.setStatus(request.status());
        auditLogService.record(AuditActionType.UPDATE, "ROOM", seat.getRoom().getId(),
                seat.getRoom().getName() + " - cập nhật ghế " + seat.getRowLabel() + seat.getSeatNumber());
        return cinemaMapper.toSeatResponse(seat);
    }

    @Transactional
    public SeatResponse deleteSeat(Long seatId) {
        Seat seat = findSeatById(seatId);
        if (seat.getSeatType() == SeatType.COUPLE) {
            findCouplePartner(seat).setStatus(SeatStatus.UNAVAILABLE);
        }
        seat.setStatus(SeatStatus.UNAVAILABLE);
        auditLogService.record(AuditActionType.DELETE, "ROOM", seat.getRoom().getId(),
                seat.getRoom().getName() + " - vô hiệu ghế " + seat.getRowLabel() + seat.getSeatNumber());
        return cinemaMapper.toSeatResponse(seat);
    }

    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room not found"));
    }

    private Seat findSeatById(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seat not found"));
    }

    private Seat findCouplePartner(Seat seat) {
        List<Seat> rowSeats = seatRepository.findByRoom(seat.getRoom())
                .stream()
                .filter(candidate -> candidate.getSeatRow().getId().equals(seat.getSeatRow().getId()))
                .sorted(Comparator.comparingInt(Seat::getDisplayColumn))
                .toList();
        if (rowSeats.size() % 2 != 0) {
            throw new BadRequestException("Couple seats require an even number of seats in row " + seat.getRowLabel());
        }
        int seatIndex = rowSeats.indexOf(seat);
        if (seatIndex < 0) {
            throw new NotFoundException("Seat not found in its row");
        }
        return rowSeats.get(seatIndex % 2 == 0 ? seatIndex + 1 : seatIndex - 1);
    }

    private void generateDefaultSeats(Room room, SeatType defaultSeatType) {
        for (int row = 0; row < room.getRowCount(); row++) {
            String rowLabel = rowLabel(row);
            SeatRow seatRow = seatRowRepository.save(new SeatRow(room, rowLabel, row + 1, 1, defaultSeatType));
            for (int column = 1; column <= room.getColumnCount(); column++) {
                seatRepository.save(new Seat(room, seatRow, column, column, defaultSeatType));
            }
        }
    }

    private void generateCustomSeats(Room room, SeatLayoutRequest request) {
        Set<String> rowLabels = new HashSet<>();
        for (SeatRowGenerationRequest rowRequest : request.rows()) {
            String rowLabel = normalizeRowLabel(rowRequest.rowLabel());
            if (!rowLabels.add(rowLabel)) {
                throw new BadRequestException("Duplicate row label: " + rowLabel);
            }
            if (rowRequest.seatNumbers().isEmpty()) {
                throw new BadRequestException("Seat numbers are required for row " + rowLabel);
            }

            SeatType rowSeatType = rowRequest.seatType() == null ? request.defaultSeatType() : rowRequest.seatType();
            SeatRow seatRow = seatRowRepository.save(new SeatRow(
                    room,
                    rowLabel,
                    rowRequest.displayOrder(),
                    rowRequest.startColumn(),
                    rowSeatType
            ));
            Set<Integer> seatNumbers = new HashSet<>();
            for (int index = 0; index < rowRequest.seatNumbers().size(); index++) {
                int seatNumber = rowRequest.seatNumbers().get(index);
                if (!seatNumbers.add(seatNumber)) {
                    throw new BadRequestException("Duplicate seat number " + seatNumber + " in row " + rowLabel);
                }
                int displayColumn = rowRequest.startColumn() + index;
                if (displayColumn > room.getColumnCount()) {
                    throw new BadRequestException("Seat layout exceeds room column count in row " + rowLabel);
                }
                seatRepository.save(new Seat(room, seatRow, seatNumber, displayColumn, rowSeatType));
            }
        }
    }

    private String normalizeRowLabel(String rowLabel) {
        return rowLabel == null ? null : rowLabel.trim().toUpperCase();
    }

    private String normalizeRoomName(String roomName) {
        return roomName.trim();
    }

    private String rowLabel(int index) {
        StringBuilder label = new StringBuilder();
        int value = index;
        do {
            label.insert(0, (char) ('A' + value % 26));
            value = value / 26 - 1;
        } while (value >= 0);
        return label.toString();
    }

}
