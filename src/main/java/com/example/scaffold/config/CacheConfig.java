package com.example.scaffold.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration
 * 
 * This configuration class sets up Redis caching with custom serializers,
 * TTL settings, and cache warming strategies.
 */
@Slf4j
@Configuration
@EnableCaching
@ConfigurationProperties(prefix = "app.cache")
public class CacheConfig {

    private Map<String, Duration> ttl = new HashMap<>();
    
    // Cache names constants
    public static final String USER_CACHE = "users";
    public static final String PRODUCT_CACHE = "products";
    public static final String CATEGORY_CACHE = "categories";
    public static final String ORDER_CACHE = "orders";
    
    /**
     * Default Redis cache configuration with JSON serialization
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues();
    }

    /**
     * Redis cache manager with custom configurations per cache
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = cacheConfiguration();
        
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // User cache - longer TTL for user data
        cacheConfigurations.put(USER_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Product cache - medium TTL for product data
        cacheConfigurations.put(PRODUCT_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Category cache - longer TTL as categories change less frequently
        cacheConfigurations.put(CATEGORY_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // Order cache - shorter TTL for order data
        cacheConfigurations.put(ORDER_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * RedisTemplate for manual cache operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // JSON serializer for values
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }

    // Configuration properties setters
    public void setTtl(Map<String, Duration> ttl) {
        this.ttl = ttl;
    }
    
    public Map<String, Duration> getTtl() {
        return ttl;
    }
}