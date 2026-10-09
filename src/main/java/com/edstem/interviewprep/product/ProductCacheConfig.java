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
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class ProductCacheConfig {

    public static final String PRODUCTS_CACHE = "products";

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
