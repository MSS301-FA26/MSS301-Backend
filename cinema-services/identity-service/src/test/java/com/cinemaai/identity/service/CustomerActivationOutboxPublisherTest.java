package com.cinemaai.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockConstruction;

import com.cinemaai.identity.entity.CustomerActivationOutboxEvent;
import com.cinemaai.identity.repository.CustomerActivationOutboxRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomerActivationOutboxPublisherTest {
    @Mock CustomerActivationOutboxRepository repository;
    @Mock RabbitTemplate rabbit;
    CustomerActivationOutboxPublisher publisher;
    CustomerActivationOutboxEvent event;

    @BeforeEach void setUp() {
        publisher = new CustomerActivationOutboxPublisher(repository, rabbit);
        event = new CustomerActivationOutboxEvent();
        ReflectionTestUtils.setField(event, "eventId", "event-71");
        ReflectionTestUtils.setField(event, "aggregateId", 71L);
        ReflectionTestUtils.setField(event, "payload", "{\"userId\":71}");
        ReflectionTestUtils.setField(event, "status", "PENDING");
        ReflectionTestUtils.setField(event, "nextAttemptAt", LocalDateTime.now().minusSeconds(1));
        when(repository.lockPending(any(), eq(10))).thenReturn(List.of(event));
    }
    @Test void ackAndRoutedMarksPublished() {
        confirmation(true, false);
        publisher.publishPending();
        assertThat(event.getStatus()).isEqualTo("PUBLISHED");
        assertThat(event.getPublishedAt()).isNotNull();
        verify(rabbit).convertAndSend(eq("cinema.identity.events"), eq("customer.activated"),
                eq(event.getPayload()), any(MessagePostProcessor.class), any(CorrelationData.class));
    }
    @Test void nackKeepsPendingWithBackoff() {
        confirmation(false, false);
        assertDeferred();
    }
    @Test void returnedMessageEvenWithAckKeepsPending() {
        confirmation(true, true);
        assertDeferred();
    }
    @Test void sendFailureKeepsPendingAndNeverAbandonsEvent() {
        ReflectionTestUtils.setField(event, "retryCount", 10);
        doThrow(new AmqpConnectException(new java.net.ConnectException("offline"))).when(rabbit)
                .convertAndSend(any(String.class), any(String.class), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));
        publisher.publishPending();
        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getRetryCount()).isEqualTo(11);
        assertThat(event.getPublishedAt()).isNull();
    }
    @Test void failedConfirmationFutureKeepsPending() {
        doAnswer(call -> {
            call.<CorrelationData>getArgument(4).getFuture().completeExceptionally(new IllegalStateException("lost"));
            return null;
        }).when(rabbit).convertAndSend(any(String.class), any(String.class), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertDeferred();
    }
    @Test void timeoutRetainsPendingWithoutRealWait() throws Exception {
        CompletableFuture<CorrelationData.Confirm> future = new CompletableFuture<>() {
            @Override public CorrelationData.Confirm get(long timeout, TimeUnit unit) throws TimeoutException {
                throw new TimeoutException("no ack");
            }
        };
        try (var construction = mockConstruction(CorrelationData.class,
                (mock, context) -> when(mock.getFuture()).thenReturn(future))) {
            assertDeferred();
        }
    }
    @Test void interruptedConfirmationRestoresInterruptAndRetainsPending() throws Exception {
        CompletableFuture<CorrelationData.Confirm> future = new CompletableFuture<>() {
            @Override public CorrelationData.Confirm get(long timeout, TimeUnit unit) throws InterruptedException {
                throw new InterruptedException("shutdown");
            }
        };
        try (var construction = mockConstruction(CorrelationData.class,
                (mock, context) -> when(mock.getFuture()).thenReturn(future))) {
            assertDeferred();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
    private void assertDeferred() {
        LocalDateTime before = LocalDateTime.now();
        publisher.publishPending();
        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(before);
        assertThat(event.getPublishedAt()).isNull();
    }
    private void confirmation(boolean ack, boolean returned) {
        doAnswer(call -> {
            var correlation = call.<CorrelationData>getArgument(4);
            if (returned) correlation.setReturned(new ReturnedMessage(new Message(new byte[0], new MessageProperties()),
                    312, "NO_ROUTE", "cinema.identity.events", "customer.activated"));
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "nack"));
            return null;
        }).when(rabbit).convertAndSend(any(String.class), any(String.class), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }
}
