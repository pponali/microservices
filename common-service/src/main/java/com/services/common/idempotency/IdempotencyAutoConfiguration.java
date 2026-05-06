package com.services.common.idempotency;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Activates idempotency support for any servlet service that:
 * <ul>
 *   <li>Has Redis on the classpath, AND</li>
 *   <li>Sets {@code services.idempotency.enabled=true} in its config (off by default — opt-in).</li>
 * </ul>
 *
 * <p>The filter is registered at very high priority (Ordered.HIGHEST_PRECEDENCE + 50)
 * so it runs before security and most other filters — we want to short-circuit replays
 * before any business logic fires.</p>
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "services.idempotency", name = "enabled", havingValue = "true")
@EnableConfigurationProperties
public class IdempotencyAutoConfiguration {

    /**
     * Dedicated RedisTemplate for idempotency records — separate from any RedisTemplate the
     * application defines so we don't fight over serializers.
     */
    @Bean
    public RedisTemplate<String, Object> idempotencyRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyStore idempotencyStore(RedisTemplate<String, Object> idempotencyRedisTemplate) {
        return new RedisIdempotencyStore(idempotencyRedisTemplate);
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(IdempotencyStore store) {
        FilterRegistrationBean<IdempotencyFilter> reg = new FilterRegistrationBean<>(new IdempotencyFilter(store));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        reg.addUrlPatterns("/api/*");
        return reg;
    }
}
