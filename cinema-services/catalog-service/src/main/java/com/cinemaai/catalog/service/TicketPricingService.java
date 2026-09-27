package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.ticket.TicketComboRequest;
import com.cinemaai.catalog.dto.request.ticket.TicketPriceValidationRequest;
import com.cinemaai.catalog.dto.request.ticket.TicketPricingRuleRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.ticket.TicketComboResponse;
import com.cinemaai.catalog.dto.response.ticket.TicketPriceValidationResponse;
import com.cinemaai.catalog.dto.response.ticket.TicketPricingRuleResponse;
import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import java.util.List;

public interface TicketPricingService {

        public List<TicketPricingRuleResponse> getRules();

        public PageResponse<TicketPricingRuleResponse> searchRules(
                TicketType ticketType,
                com.cinemaai.catalog.enums.RoomType roomType,
                SeatType seatType,
                Boolean active,
                int page,
                int size
        );

        public TicketPricingRuleResponse createRule(TicketPricingRuleRequest request);

        public TicketPricingRuleResponse updateRule(Long id, TicketPricingRuleRequest request);

        public void deleteRule(Long id);

        public List<TicketComboResponse> getCombos(boolean activeOnly);

        public PageResponse<TicketComboResponse> searchCombos(Boolean active, String keyword, int page, int size);

        public TicketComboResponse createCombo(TicketComboRequest request);

        public TicketComboResponse updateCombo(Long id, TicketComboRequest request);

        public void deleteCombo(Long id);

        public TicketPriceValidationResponse validatePrice(TicketPriceValidationRequest request);
}
