package com.cinemaai.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.repository.CustomerActivationOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class CustomerActivationOutboxServiceTest {
    @Mock CustomerActivationOutboxRepository repository;
    @Mock UserRoleService roles;
    CustomerActivationOutboxService service;
    User user;

    @BeforeEach void setUp() {
        service = new CustomerActivationOutboxService(repository, roles, new ObjectMapper().findAndRegisterModules());
        user = new User("private@example.test", "private-password-hash", "Private Name", "0900000000");
        ReflectionTestUtils.setField(user, "id", 71L);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
    }
    @AfterEach void cleanMdc() { MDC.clear(); }

    @Test void recordsOnlyMinimalMetadataWithCorrelation() throws Exception {
        when(roles.getRoleNames(71L)).thenReturn(List.of("CUSTOMER"));
        MDC.put("correlationId", "safe-request-1");
        service.recordActivation(user);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(repository).insertActivationIfAbsent(anyString(), eq(71L), payload.capture(), any());
        var json = new ObjectMapper().readTree(payload.getValue());
        assertThat(json.size()).isEqualTo(4);
        assertThat(json.get("userId").asLong()).isEqualTo(71L);
        assertThat(json.get("correlationId").asText()).isEqualTo("safe-request-1");
        assertThat(json.get("eventId").asText()).isNotBlank();
        assertThat(json.get("occurredAt")).isNotNull();
        assertThat(payload.getValue()).doesNotContain("private", "0900000000", "password", "token");
    }
    @Test void unsafeCorrelationFallsBackToEventId() throws Exception {
        when(roles.getRoleNames(71L)).thenReturn(List.of("CUSTOMER"));
        MDC.put("correlationId", "forged\nvalue");
        service.recordActivation(user);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(repository).insertActivationIfAbsent(anyString(), eq(71L), payload.capture(), any());
        var json = new ObjectMapper().readTree(payload.getValue());
        assertThat(json.get("correlationId").asText()).isEqualTo(json.get("eventId").asText());
    }
    @Test void pendingOrUnverifiedDoesNotCreateOutbox() {
        user.setEmailVerified(false);
        service.recordActivation(user);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        service.recordActivation(user);
        verifyNoInteractions(repository, roles);
    }
    @Test void nonCustomerDoesNotCreateOutbox() {
        when(roles.getRoleNames(71L)).thenReturn(List.of("MANAGER", "STAFF"));
        service.recordActivation(user);
        verifyNoInteractions(repository);
    }
    @Test void outboxFailureMustPropagateAndRollbackActivation() {
        when(roles.getRoleNames(71L)).thenReturn(List.of("CUSTOMER"));
        when(repository.insertActivationIfAbsent(anyString(), eq(71L), anyString(), any()))
                .thenThrow(new IllegalStateException("db unavailable"));
        assertThatThrownBy(() -> service.recordActivation(user)).isInstanceOf(IllegalStateException.class);
    }
    @Test void mustJoinTheAccountTransaction() throws Exception {
        assertThat(CustomerActivationOutboxService.class.getMethod("recordActivation", User.class)
                .getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
    }
}
