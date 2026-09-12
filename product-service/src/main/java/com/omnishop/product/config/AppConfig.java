package com.omnishop.product.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;
import java.time.Duration;

@EnableRabbit
@EnableCaching
@Configuration
public class AppConfig {

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

    // Stock saga participation: reserve on order.created, restore on compensation
    @Bean
    public Queue productOrderCreatedQueue() {
        return new Queue("product.order.created", true);
    }

    @Bean
    public Queue productPaymentFailedQueue() {
        return new Queue("product.order.payment_failed", true);
    }

    @Bean
    public Declarables productBindings(TopicExchange ex,
                                       Queue productOrderCreatedQueue,
                                       Queue productPaymentFailedQueue) {
        return new Declarables(
                BindingBuilder.bind(productOrderCreatedQueue).to(ex).with("order.created"),
                BindingBuilder.bind(productPaymentFailedQueue).to(ex).with("order.payment_failed"));
    }

    // Redis cache: 60s TTL is the safety net; writes evict explicitly.
    // Strategy (see README): @Cacheable on reads, @CacheEvict on every
    // mutation (product write, review add, stock change). TTL covers any
    // missed invalidation so stale data can never live longer than 60s.
    @Bean
    public RedisCacheConfiguration cacheConfiguration(
            @Value("${app.cache.ttl-seconds:60}") long ttlSeconds) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory cf, RedisCacheConfiguration conf) {
        return RedisCacheManager.builder(cf).cacheDefaults(conf).build();
    }

    @Bean("categoryKeyGenerator")
    public KeyGenerator categoryKeyGenerator() {
        return (target, method, params) -> params[0] == null ? "ALL" : String.valueOf(params[0]);
    }
}
