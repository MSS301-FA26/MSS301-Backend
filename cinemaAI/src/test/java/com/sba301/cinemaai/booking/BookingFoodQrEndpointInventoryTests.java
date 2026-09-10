package com.sba301.cinemaai.booking;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingFoodQrEndpointInventoryTests {

    @Test
    void bookingControllersShouldExposeCustomerAndAdminBookingEndpoints() throws IOException {
        String booking = readSource("controller/BookingController.java");
        for (String endpoint : List.of(
                "@requestmapping(\"/api/v1/bookings\")",
                "@postmapping(\"/hold\")",
                "@postmapping",
                "@getmapping",
                "@getmapping(\"/{bookingid}\")",
                "@deletemapping(\"/{bookingid}\")"
        )) {
            assertTrue(booking.contains(endpoint), "BookingController must expose " + endpoint);
        }

        String adminBooking = readSource("controller/AdminBookingController.java");
        for (String endpoint : List.of(
                "@requestmapping(\"/api/v1/admin/bookings\")",
                "@getmapping",
                "@getmapping(\"/{bookingid}\")",
                "@deletemapping(\"/{bookingid}\")",
                "@postmapping(\"/{bookingid}/check-in\")"
        )) {
            assertTrue(adminBooking.contains(endpoint), "AdminBookingController must expose " + endpoint);
        }
    }

    @Test
    void foodAndTicketPricingControllersShouldExposePhase5CatalogAndPricingEndpoints() throws IOException {
        String food = readSource("controller/FoodController.java");
        assertTrue(food.contains("@requestmapping(\"/api/v1/foods\")"));
        assertTrue(food.contains("@getmapping(\"/items\")"));
        assertTrue(food.contains("@getmapping(\"/combos\")"));

        String adminFood = readSource("controller/AdminFoodController.java");
        for (String endpoint : List.of(
                "@requestmapping(\"/api/v1/admin/foods\")",
                "@getmapping(\"/items\")",
                "@getmapping(\"/combos\")",
                "@postmapping(\"/items\")",
                "@postmapping(\"/combos\")",
                "@putmapping(\"/items/{itemid}\")",
                "@putmapping(\"/combos/{comboid}\")",
                "@deletemapping(\"/items/{itemid}\")",
                "@deletemapping(\"/combos/{comboid}\")"
        )) {
            assertTrue(adminFood.contains(endpoint), "AdminFoodController must expose " + endpoint);
        }

        String ticketPricing = readSource("controller/TicketPricingController.java");
        assertTrue(ticketPricing.contains("@requestmapping(\"/api/v1/ticket-pricing\")"));
        assertTrue(ticketPricing.contains("@getmapping(\"/combos\")"));
        assertTrue(ticketPricing.contains("@postmapping(\"/validate\")"));

        String adminTicketPricing = readSource("controller/AdminTicketPricingController.java");
        for (String endpoint : List.of(
                "@requestmapping(\"/api/v1/admin/ticket-pricing\")",
                "@getmapping(\"/rules\")",
                "@postmapping(\"/rules\")",
                "@putmapping(\"/rules/{ruleid}\")",
                "@deletemapping(\"/rules/{ruleid}\")",
                "@getmapping(\"/combos\")",
                "@postmapping(\"/combos\")",
                "@putmapping(\"/combos/{comboid}\")",
                "@deletemapping(\"/combos/{comboid}\")"
        )) {
            assertTrue(adminTicketPricing.contains(endpoint), "AdminTicketPricingController must expose " + endpoint);
        }
    }

    @Test
    void checkInControllerShouldExposeStaffAndAdminQrAliases() throws IOException {
        String controller = readSource("controller/StaffCheckInController.java");
        assertTrue(controller.contains("@requestmapping({\"/api/v1/staff/check-in\", \"/api/v1/admin/check-in\"})"));
        assertTrue(controller.contains("@postmapping"));
        assertTrue(controller.contains("bookingservice.checkin(request.qrcode())"));
    }

    @Test
    void phase5RequestsShouldContainSeatLockTicketFoodQrAndRefundFields() throws IOException {
        String holdSeats = readSource("dto/request/booking/HoldSeatsRequest.java");
        for (String field : List.of(
                "long showtimeid",
                "list<long> seatids",
                "long comboid",
                "boolean holiday",
                "list<ticketselectionrequest> tickets",
                "list<bookingfoodrequest> foods"
        )) {
            assertTrue(holdSeats.contains(field), "HoldSeatsRequest must include " + field);
        }

        String createBooking = readSource("dto/request/booking/CreateBookingRequest.java");
        for (String field : List.of(
                "long holdbookingid",
                "list<bookingfoodrequest> foods",
                "long comboid",
                "boolean holiday",
                "list<ticketselectionrequest> tickets"
        )) {
            assertTrue(createBooking.contains(field), "CreateBookingRequest must include " + field);
        }

        String bookingFood = readSource("dto/request/booking/BookingFoodRequest.java");
        assertTrue(bookingFood.contains("long fooditemid"));
        assertTrue(bookingFood.contains("long foodcomboid"));
        assertTrue(bookingFood.contains("int quantity"));

        String checkIn = readSource("dto/request/booking/CheckInRequest.java");
        assertTrue(checkIn.contains("@notblank(message = \"qr code is required\")"));
        assertTrue(checkIn.contains("string qrcode"));

        String refund = readSource("dto/request/booking/RefundRequest.java");
        assertTrue(refund.contains("@notblank(message = \"refund reason is required\")"));
        assertTrue(refund.contains("@size(max = 500"));
    }

    @Test
    void ticketAndFoodRequestsShouldPreservePricingAndStatusRules() throws IOException {
        String ticketSelection = readSource("dto/request/ticket/TicketSelectionRequest.java");
        for (String field : List.of(
                "long seatid",
                "tickettype tickettype",
                "seattype seattype",
                "int viewerage",
                "int quantity"
        )) {
            assertTrue(ticketSelection.contains(field), "TicketSelectionRequest must include " + field);
        }

        String priceValidation = readSource("dto/request/ticket/TicketPriceValidationRequest.java");
        assertTrue(priceValidation.contains("long showtimeid"));
        assertTrue(priceValidation.contains("long comboid"));
        assertTrue(priceValidation.contains("boolean holiday"));
        assertTrue(priceValidation.contains("list<@valid ticketselectionrequest> tickets"));

        String pricingRule = readSource("dto/request/ticket/TicketPricingRuleRequest.java");
        for (String field : List.of(
                "tickettype tickettype",
                "roomtype roomtype",
                "seattype seattype",
                "boolean weekend",
                "boolean holiday",
                "bigdecimal price",
                "boolean active"
        )) {
            assertTrue(pricingRule.contains(field), "TicketPricingRuleRequest must include " + field);
        }

        String foodItem = readSource("dto/request/food/FoodItemRequest.java");
        assertTrue(foodItem.contains("fooditemstatus status"));
        assertTrue(foodItem.contains("string imageurl"));

        String foodCombo = readSource("dto/request/food/FoodComboRequest.java");
        assertTrue(foodCombo.contains("fooditemstatus status"));
        assertTrue(foodCombo.contains("string imageurl"));
    }

    private String readSource(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/sba301/cinemaai").resolve(relativePath)).toLowerCase();
    }
}
