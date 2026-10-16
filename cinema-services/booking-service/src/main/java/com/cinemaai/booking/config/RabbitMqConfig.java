package com.cinemaai.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PAYMENT_EXCHANGE = "cinema.payment.events";
    public static final String PAYMENT_SUCCEEDED_QUEUE = "booking.payment-succeeded.queue";
    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";

    public static final String DLX_EXCHANGE = "cinema.dlx";
    public static final String PAYMENT_SUCCEEDED_DLQ = "booking.payment-succeeded.dlq";
    public static final String PAYMENT_SUCCEEDED_DLQ_ROUTING_KEY = "dlq.booking.payment.succeeded";

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange(PAYMENT_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue paymentSucceededQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCEEDED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_SUCCEEDED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentSucceededDlq() {
        return QueueBuilder.durable(PAYMENT_SUCCEEDED_DLQ).build();
    }

    @Bean
    public Binding paymentSucceededBinding(Queue paymentSucceededQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentSucceededQueue).to(paymentExchange).with(PAYMENT_SUCCEEDED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentSucceededDlqBinding(Queue paymentSucceededDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentSucceededDlq).to(deadLetterExchange).with(PAYMENT_SUCCEEDED_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
