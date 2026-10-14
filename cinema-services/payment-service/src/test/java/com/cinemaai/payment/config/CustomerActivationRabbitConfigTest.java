package com.cinemaai.payment.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

class CustomerActivationRabbitConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void activationTopologyIsDurableAndUsesTheExistingDeadLetterExchange() {
        var queue = config.customerActivatedQueue();
        var dlq = config.customerActivatedDeadLetterQueue();
        assertTrue(queue.isDurable());
        assertTrue(dlq.isDurable());
        assertEquals("cinema.dlx", config.deadLetterExchange().getName());
        assertEquals("topic", config.deadLetterExchange().getType());
        assertEquals("cinema.dlx", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMqConfig.CUSTOMER_ACTIVATED_DEAD_LETTER_ROUTING_KEY,
                queue.getArguments().get("x-dead-letter-routing-key"));
        assertEquals("customer.activated", config.customerActivatedBinding(queue, config.identityExchange()).getRoutingKey());
        assertEquals(RabbitMqConfig.CUSTOMER_ACTIVATED_DEAD_LETTER_ROUTING_KEY,
                config.customerActivatedDeadLetterBinding(dlq, config.deadLetterExchange()).getRoutingKey());
    }

    @Test
    void transientDatabaseFailureCanRecoverOnTheThirdAttempt() {
        AtomicInteger attempts = new AtomicInteger();
        TestDelivery listener = proxied(message -> {
            if (attempts.incrementAndGet() < 3) {
                throw new DataAccessResourceFailureException("test failure");
            }
        });
        listener.deliver(null, message());
        assertEquals(3, attempts.get());
    }

    @Test
    void exhaustedDatabaseFailureIsRejectedAfterExactlyThreeAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        TestDelivery listener = proxied(message -> {
            attempts.incrementAndGet();
            throw new DataAccessResourceFailureException("test failure");
        });
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.deliver(null, message()));
        assertEquals(3, attempts.get());
    }

    @Test
    void wrappedMalformedEventIsRejectedWithoutRetry() {
        AtomicInteger attempts = new AtomicInteger();
        TestDelivery listener = proxied(message -> {
            attempts.incrementAndGet();
            throw new ListenerExecutionFailedException("test wrapped failure",
                    new AmqpRejectAndDontRequeueException("invalid contract"), message);
        });
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.deliver(null, message()));
        assertEquals(1, attempts.get());
    }

    @Test
    void scopedContainerRejectsPoisonWithoutLoggingItsBodyOrExceptionMessage() {
        var factory = config.customerActivationListenerContainerFactory(
                new SimpleRabbitListenerContainerFactoryConfigurer(new RabbitProperties()),
                mock(ConnectionFactory.class), config.customerActivationRetryInterceptor());
        var container = factory.createListenerContainer(null);
        assertEquals(false, ReflectionTestUtils.invokeMethod(container, "isDefaultRequeueRejected"));
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(RabbitMqConfig.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        try {
            var message = new Message("sensitive-payload-sentinel".getBytes(StandardCharsets.UTF_8), new MessageProperties());
            var failure = new ListenerExecutionFailedException("sensitive-error-sentinel",
                    new IllegalArgumentException("sensitive-cause-sentinel"), message);
            assertThrows(AmqpRejectAndDontRequeueException.class,
                    () -> ReflectionTestUtils.invokeMethod(container, "invokeErrorHandler", failure));
            assertEquals(1, logs.list.size());
            String output = logs.list.getFirst().getFormattedMessage();
            assertTrue(output.contains("wallet_provisioning_container_rejected"));
            assertTrue(output.contains("ListenerExecutionFailedException"));
            assertFalse(output.contains("sensitive-"));
            assertEquals(null, logs.list.getFirst().getThrowableProxy());
        } finally {
            logger.detachAppender(logs);
            logs.stop();
            container.destroy();
        }
    }

    private TestDelivery proxied(MessageListener listener) {
        TestDelivery delivery = (channel, message) -> listener.onMessage(message);
        ProxyFactory factory = new ProxyFactory(delivery);
        factory.addAdvice((MethodInterceptor) config.customerActivationRetryInterceptor());
        return (TestDelivery) factory.getProxy();
    }

    private interface TestDelivery {
        void deliver(com.rabbitmq.client.Channel channel, Message message);
    }

    private Message message() {
        return new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
