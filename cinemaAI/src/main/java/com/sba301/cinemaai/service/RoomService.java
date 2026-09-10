package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.cinema.RoomRequest;
import com.sba301.cinemaai.dto.request.cinema.SeatLayoutRequest;
import com.sba301.cinemaai.dto.request.cinema.SeatUpdateRequest;
import com.sba301.cinemaai.dto.response.cinema.RoomResponse;
import com.sba301.cinemaai.dto.response.cinema.SeatResponse;
import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.enums.RoomStatus;
import java.util.List;

public interface RoomService {

        public List<RoomResponse> getRooms();

        public List<RoomResponse> getRoomsByCinema(Long cinemaId);

        public RoomResponse getRoom(Long id);

        public RoomResponse create(RoomRequest request);

        public RoomResponse update(Long id, RoomRequest request);

        public RoomResponse updateStatus(Long id, RoomStatus status);

        public List<SeatResponse> createSeats(Long roomId, SeatLayoutRequest request);

        public List<SeatResponse> replaceSeats(Long roomId, SeatLayoutRequest request);

        public List<SeatResponse> getSeats(Long roomId);

        public SeatResponse getSeat(Long seatId);

        public SeatResponse updateSeat(Long seatId, SeatUpdateRequest request);

        public SeatResponse deleteSeat(Long seatId);

        public Room findById(Long id);
}
