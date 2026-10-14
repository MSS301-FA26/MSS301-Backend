package com.cinemaai.identity.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerActivationRabbitConfig {
    public static final String IDENTITY_EXCHANGE = "cinema.identity.events";
    public static final String CUSTOMER_ACTIVATED_ROUTING_KEY = "customer.activated";

    @Bean
    public TopicExchange identityExchange() {
        return ExchangeBuilder.topicExchange(IDENTITY_EXCHANGE).durable(true).build();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // Same JSON String convention as existing Payment/Booking events.
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setMandatory(true);
        return template;
    }
}
