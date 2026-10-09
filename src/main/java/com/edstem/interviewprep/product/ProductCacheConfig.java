package com.edstem.interviewprep.product;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
// Caching runs outside the transaction: a cache hit returns without opening a transaction or borrowing a
// DB connection, and evictions happen after the transactional method has committed.
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class ProductCacheConfig {

    public static final String PRODUCTS_CACHE = "products";

    /**
     * In-process Caffeine cache, wrapped so that puts/evictions inside a transaction only happen
     * <em>after the transaction commits</em>. Without that, an eviction could run before the commit and
     * a concurrent reader could re-cache the old row, or a rolled-back update could leave the cache wrong.
     * The TTL is a safety net, not the freshness mechanism - eviction on write is.
     */
    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager caffeine = new CaffeineCacheManager(PRODUCTS_CACHE);
        caffeine.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());
        caffeine.setAllowNullValues(false);
        return new TransactionAwareCacheManagerProxy(caffeine);
    }
}
