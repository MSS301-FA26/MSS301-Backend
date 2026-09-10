package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.ticket.TicketComboRequest;
import com.sba301.cinemaai.dto.request.ticket.TicketPriceValidationRequest;
import com.sba301.cinemaai.dto.request.ticket.TicketPricingRuleRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.ticket.TicketComboResponse;
import com.sba301.cinemaai.dto.response.ticket.TicketPriceValidationResponse;
import com.sba301.cinemaai.dto.response.ticket.TicketPricingRuleResponse;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
import java.util.List;

public interface TicketPricingService {

        public List<TicketPricingRuleResponse> getRules();

        public PageResponse<TicketPricingRuleResponse> searchRules(
                TicketType ticketType,
                com.sba301.cinemaai.enums.RoomType roomType,
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
