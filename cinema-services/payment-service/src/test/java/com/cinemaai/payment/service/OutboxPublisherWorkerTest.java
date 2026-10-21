package com.cinemaai.payment.service;

import com.cinemaai.payment.entity.OutboxEvent;
import com.cinemaai.payment.enums.OutboxStatus;
import com.cinemaai.payment.repository.OutboxEventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class OutboxPublisherWorkerTest {
    @Test
    void publisherAlwaysUsesIndependentTransaction() throws Exception {
        Transactional annotation = AnnotatedElementUtils.findMergedAnnotation(
                OutboxPublisherWorker.class.getMethod("publishPendingEvents"), Transactional.class);
        assertEquals(Propagation.REQUIRES_NEW, annotation.propagation());
    }

    @Test
    void publishFailurePersistsRetryAndEventuallyFailedState() {
        OutboxEvent event = OutboxEvent.builder().id(1L).payload("payload")
                .status(OutboxStatus.PENDING).retryCount(4).build();
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of(event));
        doThrow(new IllegalStateException("broker unavailable"))
                .when(rabbit).convertAndSend(anyString(), anyString(), anyString());

        new OutboxPublisherWorker(repository, rabbit).publishPendingEvents();

        assertEquals(5, event.getRetryCount());
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        verify(repository).saveAll(anyList());
    }
}
