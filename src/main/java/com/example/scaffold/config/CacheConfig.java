package com.example.scaffold.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.example.scaffold.user.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis Cache Configuration
 *
 * This configuration class sets up Redis caching with custom serializers,
 * TTL settings, cache key generators, eviction policies, and cache warming strategies.
 */
@Slf4j
@Configuration
@EnableCaching
@ConfigurationProperties(prefix = "app.cache")
public class CacheConfig implements CachingConfigurer {

    // Cache names constants
    public static final String USER_CACHE = "users";
    public static final String USER_BY_EMAIL_CACHE = "users-by-email";
    public static final String PRODUCT_CACHE = "products";
    public static final String CATEGORY_CACHE = "categories";
    public static final String ORDER_CACHE = "orders";
    public static final String USER_SEARCH_CACHE = "user-search";

    private Map<String, Duration> ttl = new HashMap<>();

    @Value("${app.cache.key-prefix:scaffold:cache:}")
    private String keyPrefix;

    @Value("${spring.cache.redis.cache-null-values:false}")
    private boolean cacheNullValues;

    @Value("${spring.cache.redis.use-key-prefix:true}")
    private boolean useKeyPrefix;

    /**
     * Custom key generator for cache operations
     */
    @Bean
    @Primary
    public KeyGenerator customKeyGenerator() {
        return new CustomKeyGenerator();
    }

    /**
     * Custom key generator implementation
     */
    public static class CustomKeyGenerator implements KeyGenerator {
        @Override
        public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName()).append(":");
            key.append(method.getName()).append(":");

            for (Object param : params) {
                if (param != null) {
                    key.append(param.toString()).append(":");
                }
            }

            // Remove trailing colon
            if (key.length() > 0 && key.charAt(key.length() - 1) == ':') {
                key.setLength(key.length() - 1);
            }

            return key.toString();
        }
    }

    /**
     * Cache error handler to gracefully handle cache failures
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CustomCacheErrorHandler();
    }

    /**
     * Custom cache error handler implementation
     */
    public static class CustomCacheErrorHandler implements CacheErrorHandler {
        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.error("Cache get error for cache '{}' with key '{}': {}", cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.error("Cache put error for cache '{}' with key '{}': {}", cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.error("Cache evict error for cache '{}' with key '{}': {}",
                    cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.error("Cache clear error for cache '{}': {}", cache.getName(), exception.getMessage());
        }
    }

    /**
     * Redis JSON serializer for cache values. GenericJacksonJsonRedisSerializer needs default
     * typing enabled (embeds the target class in the stored JSON) or it deserializes cached
     * values back as a plain LinkedHashMap instead of the original DTO type.
     */
    @Bean
    public GenericJacksonJsonRedisSerializer redisJsonSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();
    }

    /**
     * Default Redis cache configuration with JSON serialization and TTL policies
     */
    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration(GenericJacksonJsonRedisSerializer redisJsonSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(useKeyPrefix ? keyPrefix : "")
                .entryTtl(Duration.ofMinutes(10)) // Default TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(redisJsonSerializer))
                .disableCachingNullValues(); // Don't cache null values by default
    }

    /**
     * Redis cache manager with custom configurations per cache and eviction policies
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                   GenericJacksonJsonRedisSerializer redisJsonSerializer) {
        RedisCacheConfiguration defaultConfig = defaultCacheConfiguration(redisJsonSerializer);

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Configure TTL from application properties if available, otherwise use defaults
        Duration userTtl = ttl.getOrDefault("users", Duration.ofHours(1));
        Duration productTtl = ttl.getOrDefault("products", Duration.ofMinutes(30));
        Duration categoryTtl = ttl.getOrDefault("categories", Duration.ofHours(2));
        Duration orderTtl = ttl.getOrDefault("orders", Duration.ofMinutes(15));

        // User cache - longer TTL for user data
        cacheConfigurations.put(USER_CACHE, defaultConfig.entryTtl(userTtl));

        // User by email cache - same TTL as user cache
        cacheConfigurations.put(USER_BY_EMAIL_CACHE, defaultConfig.entryTtl(userTtl));

        // User search cache - shorter TTL for search results
        cacheConfigurations.put(USER_SEARCH_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Product cache - medium TTL for product data
        cacheConfigurations.put(PRODUCT_CACHE, defaultConfig.entryTtl(productTtl));

        // Category cache - longer TTL as categories change less frequently
        cacheConfigurations.put(CATEGORY_CACHE, defaultConfig.entryTtl(categoryTtl));

        // Order cache - shorter TTL for order data
        cacheConfigurations.put(ORDER_CACHE, defaultConfig.entryTtl(orderTtl));

        // Create cache writer with lock timeout for better performance
        RedisCacheWriter cacheWriter = RedisCacheWriter
                .nonLockingRedisCacheWriter(redisConnectionFactory);

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // Enable transaction support
                .build();
    }

    /**
     * RedisTemplate for manual cache operations with enhanced serialization
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory,
                                                       GenericJacksonJsonRedisSerializer redisJsonSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // JSON serializer for values with proper Java Time support
        template.setValueSerializer(redisJsonSerializer);
        template.setHashValueSerializer(redisJsonSerializer);

        // Enable default serialization
        template.setDefaultSerializer(redisJsonSerializer);
        template.setEnableDefaultSerializer(true);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cache warming service for preloading frequently accessed data
     */
    @Bean
    public CacheWarmupService cacheWarmupService(CacheManager cacheManager,
                                                RedisTemplate<String, Object> redisTemplate,
                                                UserService userService) {
        return new CacheWarmupService(cacheManager, redisTemplate, userService);
    }

    /**
     * Cache statistics and monitoring service
     */
    @Bean
    public CacheMonitoringService cacheMonitoringService(RedisTemplate<String, Object> redisTemplate) {
        return new CacheMonitoringService(redisTemplate);
    }

    // Configuration properties setters and getters
    public void setTtl(Map<String, Duration> ttl) {
        this.ttl = ttl;
    }

    public Map<String, Duration> getTtl() {
        return ttl;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
    }

    public boolean isCacheNullValues() {
        return cacheNullValues;
    }

    public void setUseKeyPrefix(boolean useKeyPrefix) {
        this.useKeyPrefix = useKeyPrefix;
    }

    public boolean isUseKeyPrefix() {
        return useKeyPrefix;
    }
}
