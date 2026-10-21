package com.cinemaai.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMqConfig {

    public static final String PAYMENT_EXCHANGE = "cinema.payment.events";
    public static final String PAYMENT_SUCCEEDED_QUEUE = "booking.payment-succeeded.queue";
    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";
    public static final String PAYMENT_DEAD_LETTER_EXCHANGE = "cinema.payment.events.dlx";
    public static final String PAYMENT_SUCCEEDED_DEAD_LETTER_QUEUE = "booking.payment-succeeded.dlq";
    public static final String PAYMENT_SUCCEEDED_DEAD_LETTER_ROUTING_KEY = "payment.succeeded.failed";

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange(PAYMENT_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue paymentSucceededQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCEEDED_QUEUE).build();
    }

    @Bean
    public Binding paymentSucceededBinding(Queue paymentSucceededQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentSucceededQueue).to(paymentExchange).with(PAYMENT_SUCCEEDED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange paymentDeadLetterExchange() {
        return ExchangeBuilder.directExchange(PAYMENT_DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue paymentSucceededDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCEEDED_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding paymentSucceededDeadLetterBinding(
            Queue paymentSucceededDeadLetterQueue,
            DirectExchange paymentDeadLetterExchange
    ) {
        return BindingBuilder.bind(paymentSucceededDeadLetterQueue)
                .to(paymentDeadLetterExchange)
                .with(PAYMENT_SUCCEEDED_DEAD_LETTER_ROUTING_KEY);
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

    @Bean
    public RabbitTemplate paymentDeadLetterRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setChannelTransacted(true);
        return template;
    }

    @Bean
    public MessageRecoverer paymentSucceededMessageRecoverer(
            @Qualifier("paymentDeadLetterRabbitTemplate") RabbitTemplate rabbitTemplate
    ) {
        return new RepublishMessageRecoverer(
                rabbitTemplate,
                PAYMENT_DEAD_LETTER_EXCHANGE,
                PAYMENT_SUCCEEDED_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public RetryOperationsInterceptor paymentSucceededRetryInterceptor(MessageRecoverer messageRecoverer) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1_000L, 2.0, 5_000L)
                .recoverer(messageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            RetryOperationsInterceptor paymentSucceededRetryInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(paymentSucceededRetryInterceptor);
        return factory;
    }
}
