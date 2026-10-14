package com.cinemaai.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.cinemaai.identity.config.CustomerActivationRabbitConfig;
import com.cinemaai.identity.event.CustomerActivatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

class CustomerActivationRabbitConfigTest {
    @Test void preservesExistingJsonStringWireConventionAndDurability() throws Exception {
        var config = new CustomerActivationRabbitConfig();
        var exchange = config.identityExchange();
        assertThat(exchange.getName()).isEqualTo("cinema.identity.events");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.getType()).isEqualTo("topic");
        var template = config.rabbitTemplate(mock(ConnectionFactory.class));
        var payload = new ObjectMapper().findAndRegisterModules().writeValueAsString(
                new CustomerActivatedEvent("event-71", 71L, LocalDateTime.of(2026, 10, 11, 1, 2), "request-1"));
        var message = template.getMessageConverter().toMessage(payload, new MessageProperties());
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(template.getMessageConverter().fromMessage(message)).isEqualTo(payload);
        assertThat(template.isMandatoryFor(message)).isTrue();
    }
}
