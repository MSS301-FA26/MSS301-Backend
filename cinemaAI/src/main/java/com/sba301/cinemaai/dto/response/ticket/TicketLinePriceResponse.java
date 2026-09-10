package com.sba301.cinemaai.dto.response.ticket;

import com.sba301.cinemaai.enums.TicketType;
import com.sba301.cinemaai.util.MessageTranslator;
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
