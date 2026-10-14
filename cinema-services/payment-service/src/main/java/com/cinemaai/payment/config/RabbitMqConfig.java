package com.cinemaai.payment.config;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

@Configuration
@Slf4j
public class RabbitMqConfig {

    public static final String PAYMENT_EXCHANGE = "cinema.payment.events";
    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";
    public static final String IDENTITY_EXCHANGE = "cinema.identity.events";
    public static final String CUSTOMER_ACTIVATED_ROUTING_KEY = "customer.activated";
    public static final String CUSTOMER_ACTIVATED_QUEUE = "payment.customer-activated.queue";
    public static final String DEAD_LETTER_EXCHANGE = "cinema.dlx";
    public static final String CUSTOMER_ACTIVATED_DLQ = "payment.customer-activated.dlq";
    public static final String CUSTOMER_ACTIVATED_DEAD_LETTER_ROUTING_KEY = "dlq.payment.customer-activated";

    @Bean
    public TopicExchange identityExchange() {
        return ExchangeBuilder.topicExchange(IDENTITY_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue customerActivatedQueue() {
        return QueueBuilder.durable(CUSTOMER_ACTIVATED_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(CUSTOMER_ACTIVATED_DEAD_LETTER_ROUTING_KEY).build();
    }

    @Bean
    public Queue customerActivatedDeadLetterQueue() {
        return QueueBuilder.durable(CUSTOMER_ACTIVATED_DLQ).build();
    }

    @Bean
    public Binding customerActivatedBinding(@Qualifier("customerActivatedQueue") Queue queue,
            @Qualifier("identityExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(CUSTOMER_ACTIVATED_ROUTING_KEY);
    }

    @Bean
    public Binding customerActivatedDeadLetterBinding(@Qualifier("customerActivatedDeadLetterQueue") Queue queue,
            @Qualifier("deadLetterExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(CUSTOMER_ACTIVATED_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public RetryOperationsInterceptor customerActivationRetryInterceptor() {
        // Stateless retry starts a fresh service transaction on each delivery attempt.
        // https://docs.spring.io/spring-amqp/reference/3.1/amqp/resilience-recovering-from-errors-and-broker-failures.html
        return RetryInterceptorBuilder.stateless()
                .retryPolicy(new SimpleRetryPolicy(3,
                        Map.of(AmqpRejectAndDontRequeueException.class, false), true, true))
                .backOffOptions(200, 2, 1000)
                .recoverer((message, cause) -> {
                    log.error("event=wallet_provisioning_delivery_rejected entryPoint=customer_activation_consumer errorType={}",
                            cause.getClass().getSimpleName());
                    throw new AmqpRejectAndDontRequeueException("Customer activation delivery rejected");
                }).build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory customerActivationListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            @Qualifier("customerActivationRetryInterceptor") RetryOperationsInterceptor retryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(retryInterceptor);
        // The default error handler can dump the rejected message body; keep poison data out of logs.
        factory.setErrorHandler(cause -> {
            log.error("event=wallet_provisioning_container_rejected entryPoint=customer_activation_consumer errorType={}",
                    cause.getClass().getSimpleName());
            throw new AmqpRejectAndDontRequeueException("Customer activation delivery rejected");
        });
        return factory;
    }

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange(PAYMENT_EXCHANGE).durable(true).build();
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
