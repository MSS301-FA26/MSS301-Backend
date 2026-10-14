package com.cinemaai.payment.listener;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinemaai.payment.event.CustomerActivatedEvent;
import com.cinemaai.payment.service.WalletProvisioningService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class CustomerActivatedEventListenerTest {

    @Mock private WalletProvisioningService provisioning;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private CustomerActivatedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new CustomerActivatedEventListener(mapper, provisioning);
    }

    @Test
    void validCustomerEventProvisionsTheExactInternalUserId() throws Exception {
        when(provisioning.ensureWalletExists(41L)).thenReturn(true);
        listener.onCustomerActivated(payload(41L));
        verify(provisioning).ensureWalletExists(41L);
    }

    @Test
    void duplicateDeliveryIsAcknowledgedNormally() throws Exception {
        when(provisioning.ensureWalletExists(41L)).thenReturn(false);
        listener.onCustomerActivated(payload(41L));
        verify(provisioning).ensureWalletExists(41L);
    }

    @Test
    void databaseFailureIsNotSwallowedOrClassifiedAsPoison() throws Exception {
        var failure = new DataAccessResourceFailureException("test database unavailable");
        when(provisioning.ensureWalletExists(41L)).thenThrow(failure);
        String payload = payload(41L);
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> listener.onCustomerActivated(payload)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "not-json", "null", "{}",
            "{\"eventId\":\"customer-1\",\"userId\":0,\"occurredAt\":\"2026-10-11T00:00:00\"}",
            "{\"eventId\":\"customer-1\",\"userId\":-1,\"occurredAt\":\"2026-10-11T00:00:00\"}",
            "{\"eventId\":\"bad\\nevent\",\"userId\":41,\"occurredAt\":\"2026-10-11T00:00:00\"}",
            "{\"eventId\":\"customer-1\",\"userId\":41}",
            "{\"eventId\":\"customer-1\",\"userId\":41,\"occurredAt\":\"2026-10-11T00:00:00\",\"correlationId\":\"bad\\nlog\"}"})
    void malformedEventsAreRejectedWithoutDatabaseCallsOrPayloadDisclosure(String payload) {
        var exception = assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.onCustomerActivated(payload));
        org.junit.jupiter.api.Assertions.assertEquals("Invalid customer activation event", exception.getMessage());
        verifyNoInteractions(provisioning);
    }

    private String payload(Long userId) throws Exception {
        return mapper.writeValueAsString(new CustomerActivatedEvent("customer-1", userId,
                LocalDateTime.of(2026, 10, 11, 0, 0), "correlation-1"));
    }
}
