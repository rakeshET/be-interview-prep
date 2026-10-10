package com.edstem.interviewprep.controller;

import com.edstem.interviewprep.config.ProductCacheConfig;
import com.edstem.interviewprep.dto.request.ProductRequest;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.repository.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class ProductCacheTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;
    private Long productId;

    @BeforeEach
    void setUp() {
        productId = productRepository.save(new Product("Cache Test Lamp", "Home", new BigDecimal("19.99"), 5, 4.5))
                .getId();
        cacheManager.getCache(ProductCacheConfig.PRODUCTS_CACHE).clear();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void repeatedLookupsOnlyQueryTheDatabaseOnce() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/products/" + productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Cache Test Lamp"));
        }

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(cacheManager.getCache(ProductCacheConfig.PRODUCTS_CACHE).get(productId)).isNotNull();
    }

    @Test
    void updateIsVisibleImmediatelyNoStaleRead() throws Exception {
        mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.price").value(19.99));

        mockMvc.perform(put("/api/products/" + productId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductRequest("Cache Test Lamp v2", "Home", new BigDecimal("24.50"), 3, 4.8))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(jsonPath("$.name").value("Cache Test Lamp v2"))
                .andExpect(jsonPath("$.price").value(24.50))
                .andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void deletedProductIsNotServedFromTheCache() throws Exception {
        mockMvc.perform(get("/api/products/" + productId)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/products/" + productId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + productId)).andExpect(status().isNotFound());
    }

    @Test
    void failedUpdateLeavesTheCachedValueCorrect() throws Exception {
        mockMvc.perform(get("/api/products/" + productId)).andExpect(status().isOk());

        mockMvc.perform(put("/api/products/" + productId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductRequest("", "Home", new BigDecimal("1.00"), 1, 1.0))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.name").value("Cache Test Lamp"));
    }
}
