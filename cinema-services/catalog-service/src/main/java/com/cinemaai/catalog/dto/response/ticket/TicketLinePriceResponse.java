package com.cinemaai.catalog.dto.response.ticket;

import com.cinemaai.catalog.enums.TicketType;
import com.cinemaai.catalog.util.MessageTranslator;
import java.math.BigDecimal;

public record TicketLinePriceResponse(
        TicketType ticketType,
        int viewerAge,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        boolean eligible,
        String message
) {
    public TicketLinePriceResponse {
        message = MessageTranslator.translate(message);
    }
}
