package com.cinemaai.payment.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinemaai.payment.controller.PaymentController;
import com.cinemaai.payment.dto.request.CreatePaymentRequest;
import com.cinemaai.payment.dto.response.PaymentResponse;
import com.cinemaai.payment.dto.response.VNPayPaymentResponse;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.PaymentProvider;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.event.PaymentSucceededEvent;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.security.AuthenticatedUser;
import com.cinemaai.payment.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentOrderContractCompatibilityTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void legacyAndCanonicalRequestsNormalizeToSameOrderId() throws Exception {
        CreatePaymentRequest legacy = json.readValue("{\"foodOrderId\":123}", CreatePaymentRequest.class);
        CreatePaymentRequest canonical = json.readValue("{\"orderId\":123}", CreatePaymentRequest.class);
        CreatePaymentRequest equivalent = json.readValue(
                "{\"foodOrderId\":123,\"orderId\":123}", CreatePaymentRequest.class);

        assertThat(legacy.foodOrderId()).isEqualTo(123L);
        assertThat(canonical.foodOrderId()).isEqualTo(123L);
        assertThat(canonical.orderId()).isEqualTo(123L);
        assertThat(equivalent).isEqualTo(legacy);
    }

    @Test
    void conflictingRequestAliasesAreRejected() {
        assertThatThrownBy(() -> json.readValue(
                "{\"foodOrderId\":123,\"orderId\":456}", CreatePaymentRequest.class))
                .hasMessageContaining("same order");
    }

    @Test
    void responseKeepsLegacyFieldAndAddsEquivalentCanonicalAlias() throws Exception {
        PaymentResponse response = new PaymentResponse(1L, null, 123L, 7L, PaymentProvider.VNPAY,
                "TX-1", BigDecimal.TEN, PaymentStatus.SUCCESS, null, null, LocalDateTime.now(),
                null, null, LocalDateTime.now());
        JsonNode tree = json.readTree(json.writeValueAsBytes(response));

        assertThat(tree.get("foodOrderId").asLong()).isEqualTo(123L);
        assertThat(tree.get("orderId").asLong()).isEqualTo(123L);
        assertThat(tree.get("transactionId").asText()).isEqualTo("TX-1");
    }

    @Test
    void canonicalQueryAliasUsesExistingVnpayWorkflowAndRejectsConflict() {
        PaymentService service = mock(PaymentService.class);
        PaymentController controller = new PaymentController(service);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(service.createVnpayPayment(7L, null, 123L, "127.0.0.1"))
                .thenReturn(new VNPayPaymentResponse(1L, "url", "TX", BigDecimal.TEN, "VNPAY"));
        AuthenticatedUser user = new AuthenticatedUser(7L, "customer@test", List.of());

        controller.createVnpayPayment(user, null, null, null, 123L, servletRequest);

        verify(service).createVnpayPayment(7L, null, 123L, "127.0.0.1");
        assertThatThrownBy(() -> controller.createVnpayPayment(
                user, null, null, 123L, 456L, servletRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void canonicalQueryAliasUsesExistingMockPaymentWorkflow() {
        PaymentService service = mock(PaymentService.class);
        PaymentController controller = new PaymentController(service);
        AuthenticatedUser user = new AuthenticatedUser(7L, "customer@test", List.of());

        controller.mockPayment(user, null, null, null, 123L);

        verify(service).mockPayment(7L, null, 123L);
    }

    @Test
    void vnpayRejectsBodyQueryOrderConflictsBeforeCallingService() {
        PaymentService service = mock(PaymentService.class);
        PaymentController controller = new PaymentController(service);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthenticatedUser user = new AuthenticatedUser(7L, "customer@test", List.of());

        assertThatThrownBy(() -> controller.createVnpayPayment(
                user, new CreatePaymentRequest(null, 123L), null, 456L, null, request))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> controller.createVnpayPayment(
                user, new CreatePaymentRequest(null, 123L), null, null, 456L, request))
                .isInstanceOf(BadRequestException.class);

        verify(service, never()).createVnpayPayment(any(), any(), any(), any());
    }

    @Test
    void mockRejectsBodyQueryOrderConflictsBeforeCallingService() {
        PaymentService service = mock(PaymentService.class);
        PaymentController controller = new PaymentController(service);
        AuthenticatedUser user = new AuthenticatedUser(7L, "customer@test", List.of());

        assertThatThrownBy(() -> controller.mockPayment(
                user, new CreatePaymentRequest(null, 123L), null, 456L, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> controller.mockPayment(
                user, new CreatePaymentRequest(null, 123L), null, null, 456L))
                .isInstanceOf(BadRequestException.class);

        verify(service, never()).mockPayment(any(), any(), any());
    }

    @Test
    void equivalentAndSingleOrderAliasesReachBothPaymentWorkflows() {
        PaymentService service = mock(PaymentService.class);
        PaymentController controller = new PaymentController(service);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        AuthenticatedUser user = new AuthenticatedUser(7L, "customer@test", List.of());

        controller.createVnpayPayment(
                user, new CreatePaymentRequest(null, 123L), null, null, 123L, request);
        controller.createVnpayPayment(user, null, null, 123L, null, request);
        controller.createVnpayPayment(user, null, null, null, 123L, request);
        controller.mockPayment(user, new CreatePaymentRequest(null, 123L), null, 123L, null);
        controller.mockPayment(user, null, null, 123L, null);
        controller.mockPayment(user, null, null, null, 123L);

        verify(service, times(3)).createVnpayPayment(7L, null, 123L, "127.0.0.1");
        verify(service, times(3)).mockPayment(7L, null, 123L);
    }

    @Test
    void persistenceAndRepositoryKeepLegacyFoodOrderMapping() throws Exception {
        assertThat(Payment.class.getDeclaredField("foodOrderId").getAnnotation(Column.class).name())
                .isEqualTo("food_order_id");
        assertThat(PaymentRepository.class.getMethod("findByFoodOrderId", Long.class)).isNotNull();
        assertThat(PaymentRepository.class.getMethod(
                "findByFoodOrderIdAndStatus", Long.class, PaymentStatus.class)).isNotNull();
    }

    @Test
    void paymentProducerPayloadRemainsLegacyForOldConsumers() throws Exception {
        PaymentSucceededEvent event = new PaymentSucceededEvent("evt", 1L, null, 123L, 7L,
                BigDecimal.TEN, "VNPAY", LocalDateTime.now());
        JsonNode tree = json.readTree(json.writeValueAsBytes(event));

        assertThat(tree.get("foodOrderId").asLong()).isEqualTo(123L);
        assertThat(tree.has("orderId")).isFalse();
    }
}
