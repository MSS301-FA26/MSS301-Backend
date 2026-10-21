package com.cinemaai.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@EnabledIfEnvironmentVariable(named = "CK06_RABBIT_PORT", matches = "\\d+")
class RabbitMqPoisonMessageIntegrationTest {

    private final RabbitMqConfig config = new RabbitMqConfig();
    private CachingConnectionFactory connectionFactory;
    private RabbitAdmin admin;
    private RabbitTemplate publisher;

    @BeforeEach
    void connectToDisposableBroker() {
        connectionFactory = new CachingConnectionFactory(
                "127.0.0.1", Integer.parseInt(System.getenv("CK06_RABBIT_PORT")));
        connectionFactory.setUsername("ck06");
        connectionFactory.setPassword("ck06-test-only");
        connectionFactory.setVirtualHost("/ck06-b4");
        admin = new RabbitAdmin(connectionFactory);
        publisher = new RabbitTemplate(connectionFactory);
    }

    @AfterEach
    void closeConnection() {
        connectionFactory.destroy();
    }

    @Test
    void legacyQueueAcceptsNewDeclarationsAndPoisonMessageIsRetainedInDlq() throws Exception {
        Queue legacyQueue = new Queue(RabbitMqConfig.PAYMENT_SUCCEEDED_QUEUE, true, false, false);
        admin.declareQueue(legacyQueue);
        declareProductionTopology();

        Properties mainProperties = admin.getQueueProperties(RabbitMqConfig.PAYMENT_SUCCEEDED_QUEUE);
        assertThat(mainProperties).isNotNull();

        AtomicInteger poisonAttempts = new AtomicInteger();
        CountDownLatch validProcessed = new CountDownLatch(1);
        RabbitTemplate recoveryTemplate = config.paymentDeadLetterRabbitTemplate(connectionFactory);
        RetryOperationsInterceptor retry = config.paymentSucceededRetryInterceptor(
                config.paymentSucceededMessageRecoverer(recoveryTemplate));
        SimpleMessageListenerContainer listener = listenerFor(
                RabbitMqConfig.PAYMENT_SUCCEEDED_QUEUE,
                retry,
                message -> {
                    String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                    if (payload.contains("\"foodOrderId\":8") && payload.contains("\"orderId\":9")) {
                        poisonAttempts.incrementAndGet();
                        throw new IllegalArgumentException("conflicting aliases");
                    }
                    validProcessed.countDown();
                });

        String poison = "{\"eventId\":\"evt-poison\",\"foodOrderId\":8,\"orderId\":9}";
        String valid = "{\"eventId\":\"evt-valid\",\"foodOrderId\":8}";
        try {
            listener.start();
            publish(RabbitMqConfig.PAYMENT_EXCHANGE, RabbitMqConfig.PAYMENT_SUCCEEDED_ROUTING_KEY, poison);
            Message deadLetter = receiveEventually(RabbitMqConfig.PAYMENT_SUCCEEDED_DEAD_LETTER_QUEUE);

            assertThat(poisonAttempts).hasValue(3);
            assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).isEqualTo(poison);
            assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).contains("\"eventId\":\"evt-poison\"");
            assertThat(messageCount(RabbitMqConfig.PAYMENT_SUCCEEDED_QUEUE)).isZero();

            publish(RabbitMqConfig.PAYMENT_EXCHANGE, RabbitMqConfig.PAYMENT_SUCCEEDED_ROUTING_KEY, valid);
            assertThat(validProcessed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(messageCount(RabbitMqConfig.PAYMENT_SUCCEEDED_QUEUE)).isZero();
        } finally {
            listener.stop();
        }
    }

    @Test
    void failedDlqRepublishDoesNotAcknowledgeOriginalDelivery() throws Exception {
        String queueName = "booking.payment-succeeded.recovery-failure.queue";
        admin.declareQueue(new Queue(queueName, true, false, false));
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch firstCycle = new CountDownLatch(3);
        RabbitTemplate transactionalTemplate = config.paymentDeadLetterRabbitTemplate(connectionFactory);
        RetryOperationsInterceptor retry = config.paymentSucceededRetryInterceptor(
                new RepublishMessageRecoverer(
                        transactionalTemplate,
                        "missing.ck06.exchange",
                        RabbitMqConfig.PAYMENT_SUCCEEDED_DEAD_LETTER_ROUTING_KEY));
        SimpleMessageListenerContainer listener = listenerFor(queueName, retry, message -> {
            attempts.incrementAndGet();
            firstCycle.countDown();
            throw new IllegalArgumentException("poison");
        });

        try {
            listener.start();
            publish("", queueName, "{\"eventId\":\"evt-recovery-failure\"}");
            assertThat(firstCycle.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            listener.stop();
        }

        assertThat(attempts.get()).isGreaterThanOrEqualTo(3);
        assertThat(messageCount(queueName)).isEqualTo(1);
    }

    private void declareProductionTopology() {
        admin.declareExchange(config.paymentExchange());
        admin.declareBinding(config.paymentSucceededBinding(
                config.paymentSucceededQueue(), config.paymentExchange()));
        DirectExchange deadLetterExchange = config.paymentDeadLetterExchange();
        Queue deadLetterQueue = config.paymentSucceededDeadLetterQueue();
        Binding deadLetterBinding = config.paymentSucceededDeadLetterBinding(deadLetterQueue, deadLetterExchange);
        admin.declareExchange(deadLetterExchange);
        admin.declareQueue(deadLetterQueue);
        admin.declareBinding(deadLetterBinding);
    }

    private SimpleMessageListenerContainer listenerFor(
            String queue,
            RetryOperationsInterceptor retry,
            org.springframework.amqp.core.MessageListener messageListener
    ) {
        SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer(connectionFactory);
        listener.setQueueNames(queue);
        listener.setAcknowledgeMode(AcknowledgeMode.AUTO);
        listener.setDefaultRequeueRejected(true);
        listener.setAdviceChain(retry);
        listener.setMessageListener(messageListener);
        listener.setReceiveTimeout(Duration.ofMillis(100).toMillis());
        return listener;
    }

    private void publish(String exchange, String routingKey, String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        publisher.send(exchange, routingKey,
                new Message(body.getBytes(StandardCharsets.UTF_8), properties));
    }

    private Message receiveEventually(String queue) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            Message message = publisher.receive(queue, 100);
            if (message != null) {
                return message;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No message arrived in " + queue);
    }

    private int messageCount(String queue) {
        Properties properties = admin.getQueueProperties(queue);
        return (Integer) properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
    }
}
