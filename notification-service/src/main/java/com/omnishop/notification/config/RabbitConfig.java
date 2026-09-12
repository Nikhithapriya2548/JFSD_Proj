package com.omnishop.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.*;

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

    // One queue, bound to every notification-worthy event
    @Bean
    public Queue notifyEventsQueue() {
        return new Queue("notify.events", true);
    }

    @Bean
    public Declarables notifyBindings(TopicExchange ex, Queue notifyEventsQueue) {
        return new Declarables(
                BindingBuilder.bind(notifyEventsQueue).to(ex).with("order.created"),
                BindingBuilder.bind(notifyEventsQueue).to(ex).with("order.status.changed"),
                BindingBuilder.bind(notifyEventsQueue).to(ex).with("payment.completed"),
                BindingBuilder.bind(notifyEventsQueue).to(ex).with("payment.failed"),
                BindingBuilder.bind(notifyEventsQueue).to(ex).with("order.payment_failed"),
                BindingBuilder.bind(notifyEventsQueue).to(ex).with("stock.low"));
    }
}
