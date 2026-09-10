package com.sba301.cinemaai.payment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentEndpointInventoryTests {

    @Test
    void paymentControllerShouldExposePhase6Endpoints() throws IOException {
        String controller = readSource("controller/PaymentController.java");

        assertTrue(controller.contains("@requestmapping(\"/api/v1/payments\")"));
        for (String endpoint : List.of(
                "@postmapping(\"/vnpay/create\")",
                "@getmapping(\"/vnpay/return\")",
                "@getmapping(\"/vnpay/ipn\")",
                "@postmapping(\"/mock\")",
                "@getmapping(\"/booking/{bookingid}\")"
        )) {
            assertTrue(controller.contains(endpoint), "PaymentController must expose " + endpoint);
        }
    }

    @Test
    void paymentServiceShouldPreservePaymentStateMachineAndVnpaySafetyChecks() throws IOException {
        String service = readSource("service/impl/PaymentServiceImpl.java");

        for (String rule : List.of(
                "bookingstatus.holding",
                "bookingstatus.pending_payment",
                "paymentstatus.pending",
                "paymentstatus.success",
                "paymentprovider.vnpay",
                "paymentprovider.mock",
                "a pending payment already exists for this booking",
                "only holding or pending_payment bookings can be paid",
                "markpaid(booking, qrticketservice.generate(booking))",
                "loyaltypointservice.addpointsfrombooking",
                "return \"{\\\"rspcode\\\":\\\"02\\\",\\\"message\\\":\\\"order already confirmed\\\"}\"",
                "return \"{\\\"rspcode\\\":\\\"04\\\",\\\"message\\\":\\\"invalid amount\\\"}\""
        )) {
            assertTrue(service.contains(rule), "PaymentService must preserve rule " + rule);
        }
    }

    @Test
    void vnpayServiceShouldSignAndVerifyCallbacks() throws IOException {
        String service = readSource("service/impl/VNPayServiceImpl.java");
        assertTrue(service.contains("vnp_version"));
        assertTrue(service.contains("vnp_command"));
        assertTrue(service.contains("vnp_amount"));
        assertTrue(service.contains("vnp_txnref"));
        assertTrue(service.contains("vnp_returnurl"));
        assertTrue(service.contains("vnp_securehash"));
        assertTrue(service.contains("verifysignature"));
        assertTrue(service.contains("filtered.remove(\"vnp_securehash\")"));
        assertTrue(service.contains("filtered.remove(\"vnp_securehashtype\")"));
    }

    @Test
    void paymentEntityAndResponseShouldContainPhase6Fields() throws IOException {
        String entity = readSource("entity/Payment.java");
        for (String field : List.of(
                "booking booking",
                "paymentprovider provider",
                "string transactionid",
                "bigdecimal amount",
                "paymentstatus status",
                "localdatetime paidat",
                "string callbackpayload"
        )) {
            assertTrue(entity.contains(field), "Payment entity must include " + field);
        }

        String response = readSource("dto/response/payment/PaymentResponse.java");
        for (String field : List.of(
                "long id",
                "long bookingid",
                "paymentprovider provider",
                "string transactionid",
                "bigdecimal amount",
                "paymentstatus status",
                "string paymenturl",
                "localdatetime paidat",
                "localdatetime createdat"
        )) {
            assertTrue(response.contains(field), "PaymentResponse must include " + field);
        }
    }

    private String readSource(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/sba301/cinemaai").resolve(relativePath)).toLowerCase();
    }
}
