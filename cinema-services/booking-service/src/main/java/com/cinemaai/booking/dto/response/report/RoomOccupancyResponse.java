package com.cinemaai.booking.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoomOccupancyResponse {

    private Long roomId;
    private String roomName;
    private long ticketsSold;
    private long roomCapacity; // tá»•ng gháº¿ cÃ³ thá»ƒ bÃ¡n = sá»©c chá»©a phÃ²ng Ã— sá»‘ suáº¥t trong khoáº£ng
    private double occupancyRate;
    private long totalShowtimes;
}

