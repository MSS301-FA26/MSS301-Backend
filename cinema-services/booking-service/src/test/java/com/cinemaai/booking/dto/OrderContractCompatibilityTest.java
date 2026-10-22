package com.cinemaai.booking.dto;

import com.cinemaai.booking.client.dto.CatalogProductQuoteDto;
import com.cinemaai.booking.dto.request.OrderRequest;
import com.cinemaai.booking.dto.response.OrderItemResponse;
import com.cinemaai.booking.dto.response.OrderResponse;
import com.cinemaai.booking.entity.BookingProductItem;
import com.cinemaai.booking.entity.Order;
import com.cinemaai.booking.entity.OrderItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderContractCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void legacyOrderRequestJsonKeepsFoodAndProductIdentityFields() throws Exception {
        OrderRequest request = objectMapper.readValue("""
                {
                  "foods": [
                    {"foodItemId": 11, "foodComboId": null, "productId": 11, "isCombo": false, "quantity": 2},
                    {"foodItemId": null, "foodComboId": 11, "productId": 11, "isCombo": true, "quantity": 1}
                  ],
                  "promotionCode": "SAVE10",
                  "cinemaId": 7
                }
                """, OrderRequest.class);

        assertEquals(2, request.foods().size());
        assertEquals(11L, request.foods().get(0).productId());
        assertFalse(request.foods().get(0).isCombo());
        assertEquals(11L, request.foods().get(1).productId());
        assertTrue(request.foods().get(1).isCombo());
    }

    @Test
    void renamedResponseKeepsLegacyAndGenericJsonFields() throws Exception {
        OrderItemResponse item = new OrderItemResponse(
                1L, 11L, null, 11L, true, "Combo", "Combo", 1,
                new BigDecimal("75000"), new BigDecimal("75000"));
        OrderResponse response = new OrderResponse(
                9L, "FO-9", "FO-9", null, null, "PAID",
                new BigDecimal("75000"), new BigDecimal("75000"),
                null, null, null, null, "FOOD:FO-9:9", List.of(item));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response));

        assertEquals("FO-9", json.get("orderCode").asText());
        assertEquals("FO-9", json.get("foodOrderCode").asText());
        assertEquals(11L, json.get("items").get(0).get("productId").asLong());
        assertEquals(11L, json.get("items").get(0).get("foodComboId").asLong());
        assertTrue(json.get("items").get(0).get("isCombo").asBoolean());
    }

    @Test
    void renamedEntitiesKeepLegacyPhysicalMappings() throws Exception {
        assertEquals("food_orders", Order.class.getAnnotation(Table.class).name());
        assertEquals("food_order_items", OrderItem.class.getAnnotation(Table.class).name());
        assertEquals("booking_food_items", BookingProductItem.class.getAnnotation(Table.class).name());
        assertEquals("food_order_code",
                Order.class.getDeclaredField("foodOrderCode").getAnnotation(Column.class).name());
        assertEquals("food_order_id",
                OrderItem.class.getDeclaredField("order").getAnnotation(JoinColumn.class).name());
    }

    @Test
    void bookingCatalogClientContinuesSendingLegacyFoodsContract() throws Exception {
        var request = new CatalogProductQuoteDto.Request(List.of(
                new CatalogProductQuoteDto.Request.Item(null, null, 11L, false, 2)));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(request));

        assertTrue(json.has("foods"));
        assertFalse(json.has("products"));
        assertEquals(11L, json.get("foods").get(0).get("productId").asLong());
        assertFalse(json.get("foods").get(0).get("isCombo").asBoolean());
    }
}
