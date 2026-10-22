package com.cinemaai.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinemaai.catalog.dto.request.quote.CheckoutQuoteRequest;
import com.cinemaai.catalog.dto.response.product.ProductQuoteResponse;
import com.cinemaai.catalog.dto.response.quote.CheckoutQuoteResponse;
import com.cinemaai.catalog.enums.TicketType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductContractJsonCompatibilityTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void checkoutAcceptsEquivalentLegacyAndCanonicalSelections() throws Exception {
        String common = """
                "showtimeId":40,"seatIds":[100],
                "tickets":[{"seatId":100,"ticketType":"ADULT","viewerAge":22,"quantity":1}]
                """;
        CheckoutQuoteRequest legacy = mapper.readValue("{" + common
                + ",\"foods\":[{\"foodItemId\":50,\"quantity\":2}]}", CheckoutQuoteRequest.class);
        CheckoutQuoteRequest canonical = mapper.readValue("{" + common
                + ",\"products\":[{\"foodItemId\":50,\"quantity\":2}]}", CheckoutQuoteRequest.class);

        assertThat(canonical.foods()).isEqualTo(legacy.foods());
        assertThat(canonical.foods()).singleElement().satisfies(selection -> {
            assertThat(selection.productId()).isEqualTo(50L);
            assertThat(selection.isCombo()).isFalse();
            assertThat(selection.quantity()).isEqualTo(2);
        });
    }

    @Test
    void checkoutRejectsDualFieldsWithoutDependingOnJsonOrder() {
        String legacyFirst = """
                {"showtimeId":40,"seatIds":[100],"foods":[],"products":[]}
                """;
        String canonicalFirst = """
                {"showtimeId":40,"seatIds":[100],"products":[],"foods":[]}
                """;

        assertThatThrownBy(() -> mapper.readValue(legacyFirst, CheckoutQuoteRequest.class))
                .hasMessageContaining("either foods or products");
        assertThatThrownBy(() -> mapper.readValue(canonicalFirst, CheckoutQuoteRequest.class))
                .hasMessageContaining("either foods or products");
    }

    @Test
    void serializationKeepsLegacyCheckoutAndProductQuoteResponseFields() throws Exception {
        var product = new CheckoutQuoteResponse.ProductSnapshot(
                50L, false, "Popcorn", new BigDecimal("45000"), 2, new BigDecimal("90000"));
        var response = new CheckoutQuoteResponse(
                "quote", java.time.Instant.EPOCH, null, List.of(),
                List.of(new CheckoutQuoteResponse.TicketSnapshot(100L, TicketType.ADULT, 1,
                        new BigDecimal("90000"), new BigDecimal("90000"))),
                List.of(product), new BigDecimal("90000"), new BigDecimal("90000"),
                new BigDecimal("180000"));
        String checkoutJson = mapper.writeValueAsString(response);
        String productJson = mapper.writeValueAsString(new ProductQuoteResponse(
                List.of(new ProductQuoteResponse.ProductSnapshot(50L, false, "Popcorn",
                        new BigDecimal("45000"), 2, new BigDecimal("90000"))),
                new BigDecimal("90000")));

        assertThat(checkoutJson).contains("\"foods\"", "\"foodItems\"", "\"foodSubtotal\"");
        assertThat(checkoutJson).doesNotContain("\"products\"");
        assertThat(productJson).contains("\"items\"", "\"totalAmount\"", "\"isCombo\"");
    }
}
