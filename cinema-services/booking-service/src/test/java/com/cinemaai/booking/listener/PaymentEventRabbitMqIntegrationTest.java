package com.cinemaai.booking.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.event.PaymentSucceededEvent;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.mockito.Mockito;

class PaymentEventRabbitMqIntegrationTest {

    private static final String CONTAINER_NAME = "booking-rabbit-test-" + UUID.randomUUID();
    private static int rabbitPort;

    @BeforeAll
    static void startRabbitMq() throws Exception {
        runDocker("run", "--rm", "--detach", "--publish-all", "--name", CONTAINER_NAME, "rabbitmq:3.13-alpine");
        rabbitPort = rabbitPort();
        waitForRabbitMq();
    }

    @AfterAll
    static void stopRabbitMq() throws Exception {
        runDocker("rm", "--force", CONTAINER_NAME);
    }

    @Test
    void deliversPaymentSucceededMessageToBookingListener() throws Exception {
        String queueName = "booking-payment-succeeded-test-" + UUID.randomUUID();
        BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
        ProcessedEventRepository processedEventRepository = Mockito.mock(ProcessedEventRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PaymentEventListener listener = new PaymentEventListener(
                bookingRepository, processedEventRepository, objectMapper);
        Booking booking = Booking.builder()
                .id(99L)
                .bookingCode("BK99")
                .status(BookingStatus.PENDING_PAYMENT)
                .build();
        when(processedEventRepository.claimEvent("payment-succeeded-10", "PaymentSucceededEvent"))
                .thenReturn(1);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost", rabbitPort);
        try {
            RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
            rabbitTemplate.execute(channel -> {
                channel.queueDeclare(queueName, false, false, true, null);
                return null;
            });

            CountDownLatch delivered = new CountDownLatch(1);
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
            container.setQueueNames(queueName);
            container.setMessageListener((Message message) -> {
                listener.onPaymentSucceeded(new String(message.getBody(), StandardCharsets.UTF_8));
                delivered.countDown();
            });
            container.afterPropertiesSet();
            container.start();
            try {
                String payload = objectMapper.writeValueAsString(new PaymentSucceededEvent(
                        "payment-succeeded-10", 10L, 99L, null, 7L,
                        BigDecimal.valueOf(150_000), "VNPAY", LocalDateTime.now()));
                rabbitTemplate.convertAndSend("", queueName, payload);

                assertTrue(delivered.await(10, TimeUnit.SECONDS));
                assertEquals(BookingStatus.PAID, booking.getStatus());
            } finally {
                container.stop();
                container.destroy();
            }
        } finally {
            connectionFactory.destroy();
        }
    }

    private static void waitForRabbitMq() throws Exception {
        for (int attempt = 0; attempt < 60; attempt++) {
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost", rabbitPort);
            try {
                org.springframework.amqp.rabbit.connection.Connection connection = connectionFactory.createConnection();
                connection.close();
                return;
            } catch (Exception ignored) {
                Thread.sleep(500);
            } finally {
                connectionFactory.destroy();
            }
        }
        throw new IllegalStateException("RabbitMQ test container did not become ready");
    }

    private static int rabbitPort() throws Exception {
        String output = runDocker("port", CONTAINER_NAME, "5672/tcp").trim();
        return Integer.parseInt(output.substring(output.lastIndexOf(':') + 1));
    }

    private static String runDocker(String... arguments) throws Exception {
        Process process = new ProcessBuilder(concat("docker", arguments))
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        if (process.waitFor() != 0) {
            throw new IllegalStateException("Docker command failed: " + output);
        }
        return output;
    }

    private static String[] concat(String first, String... rest) {
        String[] command = new String[rest.length + 1];
        command[0] = first;
        System.arraycopy(rest, 0, command, 1, rest.length);
        return command;
    }
}
