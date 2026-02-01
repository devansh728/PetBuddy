package com.petbuddy.social.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for graceful cache error handling.
 * When Redis is unavailable, the application will continue to work
 * without caching instead of failing.
 */
@Configuration
@Slf4j
public class CacheErrorConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache get error for cache '{}', key '{}': {}", 
                        cache.getName(), key, exception.getMessage());
                // Don't rethrow - continue without cache
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache put error for cache '{}', key '{}': {}", 
                        cache.getName(), key, exception.getMessage());
                // Don't rethrow - continue without cache
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache evict error for cache '{}', key '{}': {}", 
                        cache.getName(), key, exception.getMessage());
                // Don't rethrow - continue without cache
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache clear error for cache '{}': {}", 
                        cache.getName(), exception.getMessage());
                // Don't rethrow - continue without cache
            }
        };
    }
}
