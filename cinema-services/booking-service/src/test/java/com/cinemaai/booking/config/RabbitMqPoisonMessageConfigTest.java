package com.cinemaai.booking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

class RabbitMqPoisonMessageConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void existingMainQueueDeclarationRemainsCompatible() {
        Queue queue = config.paymentSucceededQueue();

        assertThat(queue.getName()).isEqualTo(RabbitMqConfig.PAYMENT_SUCCEEDED_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments()).isEmpty();
    }

    @Test
    void declaresDurableDeadLetterQueueAndBinding() {
        var exchange = config.paymentDeadLetterExchange();
        Queue queue = config.paymentSucceededDeadLetterQueue();
        Binding binding = config.paymentSucceededDeadLetterBinding(queue, exchange);

        assertThat(exchange.getName()).isEqualTo(RabbitMqConfig.PAYMENT_DEAD_LETTER_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(queue.getName()).isEqualTo(RabbitMqConfig.PAYMENT_SUCCEEDED_DEAD_LETTER_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(binding.getExchange()).isEqualTo(RabbitMqConfig.PAYMENT_DEAD_LETTER_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqConfig.PAYMENT_SUCCEEDED_DEAD_LETTER_ROUTING_KEY);
    }

    @Test
    void deadLetterPublishUsesBrokerTransaction() {
        RabbitTemplate template = config.paymentDeadLetterRabbitTemplate(mock(ConnectionFactory.class));

        assertThat(template.isChannelTransacted()).isTrue();
    }

    @Test
    void exhaustedRetryRepublishesOriginalPayloadAfterThreeAttempts() throws Throwable {
        RabbitTemplate template = mock(RabbitTemplate.class);
        MessageRecoverer recoverer = config.paymentSucceededMessageRecoverer(template);
        RetryOperationsInterceptor interceptor = config.paymentSucceededRetryInterceptor(recoverer);
        ProxyMethodInvocation invocation = mock(ProxyMethodInvocation.class);
        byte[] rawPayload = "{\"eventId\":\"bad\",\"foodOrderId\":8,\"orderId\":9}"
                .getBytes(StandardCharsets.UTF_8);
        Message original = new Message(rawPayload, new MessageProperties());
        when(invocation.getArguments()).thenReturn(new Object[]{null, original});
        when(invocation.getMethod()).thenReturn(
                ListenerInvocation.class.getDeclaredMethod("handle", Object.class, Message.class));
        when(invocation.invocableClone()).thenReturn(invocation);
        when(invocation.proceed()).thenThrow(new IllegalArgumentException("conflicting aliases"));

        interceptor.invoke(invocation);

        verify(invocation, times(3)).proceed();
        ArgumentCaptor<Message> deadLetter = ArgumentCaptor.forClass(Message.class);
        verify(template).send(
                eq(RabbitMqConfig.PAYMENT_DEAD_LETTER_EXCHANGE),
                eq(RabbitMqConfig.PAYMENT_SUCCEEDED_DEAD_LETTER_ROUTING_KEY),
                deadLetter.capture());
        assertThat(deadLetter.getValue().getBody()).isEqualTo(rawPayload);
    }

    public static final class ListenerInvocation {
        public void handle(Object channel, Message message) {
        }
    }
}
