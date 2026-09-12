package com.omnishop.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.*;

@EnableRabbit
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "omnishop.events";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(conv);
        return t;
    }

    // Payment results routed back to order-service
    @Bean
    public Queue orderPaymentResultsQueue() {
        return new Queue("order.payment.results", true);
    }

    @Bean
    public Declarables orderResultBindings(TopicExchange ex, Queue orderPaymentResultsQueue) {
        return new Declarables(
                BindingBuilder.bind(orderPaymentResultsQueue).to(ex).with("payment.completed"),
                BindingBuilder.bind(orderPaymentResultsQueue).to(ex).with("payment.failed"));
    }
}
