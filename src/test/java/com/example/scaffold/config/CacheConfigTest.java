package com.example.scaffold.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Cache Configuration
 */
class CacheConfigTest {

    @Test
    void cacheNamesAreCorrect() {
        assertEquals("users", CacheConfig.USER_CACHE);
        assertEquals("products", CacheConfig.PRODUCT_CACHE);
        assertEquals("categories", CacheConfig.CATEGORY_CACHE);
        assertEquals("orders", CacheConfig.ORDER_CACHE);
    }
}