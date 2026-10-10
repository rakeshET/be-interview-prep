package com.edstem.interviewprep.controller;

import com.edstem.interviewprep.config.ProductSeeder;
import com.edstem.interviewprep.dto.request.ProductRequest;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.repository.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ProductCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void catalogIsSeededAndPageReportsTotalCountAndPages() throws Exception {
        long total = productRepository.count();
        assertThat(total).isGreaterThanOrEqualTo(ProductSeeder.PRODUCT_COUNT);

        mockMvc.perform(get("/api/products").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value((int) Math.ceil(total / 20.0)));
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content.length()").value(100));
    }

    @Test
    void sortsByAnyAllowedField() throws Exception {
        for (String field : List.of("price", "rating", "stock", "name", "createdAt", "category")) {
            mockMvc.perform(get("/api/products").param("sort", field + ",desc").param("size", "5"))
                    .andExpect(status().isOk());
        }
        JsonNode content = content(get("/api/products").param("sort", "price,desc").param("size", "100"));
        List<BigDecimal> prices = new ArrayList<>();
        content.forEach(p -> prices.add(p.get("price").decimalValue()));
        assertThat(prices).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void unknownSortFieldReturns400InsteadOf500() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
    }

    @Test
    void allFiltersCombineInASingleRequest() throws Exception {
        Product sample = productRepository.findAll().stream()
                .filter(p -> p.getStock() > 0).findFirst().orElseThrow();
        String category = sample.getCategory();
        BigDecimal min = sample.getPrice().subtract(BigDecimal.valueOf(100)).max(BigDecimal.ZERO);
        BigDecimal max = sample.getPrice().add(BigDecimal.valueOf(100));
        String q = sample.getName().split(" ")[1].toLowerCase(Locale.ROOT);

        JsonNode content = content(get("/api/products")
                .param("category", category.toUpperCase(Locale.ROOT))
                .param("minPrice", min.toPlainString())
                .param("maxPrice", max.toPlainString())
                .param("inStock", "true")
                .param("q", q)
                .param("size", "100"));

        long expected = productRepository.findAll().stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .filter(p -> p.getPrice().compareTo(min) >= 0 && p.getPrice().compareTo(max) <= 0)
                .filter(p -> p.getStock() > 0)
                .filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(q))
                .count();
        assertThat(expected).isPositive();
        assertThat(content.size()).isEqualTo((int) expected);
        content.forEach(p -> {
            assertThat(p.get("category").asText()).isEqualToIgnoringCase(category);
            assertThat(p.get("price").decimalValue()).isBetween(min, max);
            assertThat(p.get("stock").asInt()).isPositive();
            assertThat(p.get("name").asText().toLowerCase(Locale.ROOT)).contains(q);
        });
    }

    @Test
    void eachFilterAlsoWorksOnItsOwn() throws Exception {
        content(get("/api/products").param("inStock", "true").param("size", "100"))
                .forEach(p -> assertThat(p.get("stock").asInt()).isPositive());
        content(get("/api/products").param("maxPrice", "50").param("size", "100"))
                .forEach(p -> assertThat(p.get("price").decimalValue()).isLessThanOrEqualTo(new BigDecimal("50")));
        content(get("/api/products").param("category", "Books").param("size", "100"))
                .forEach(p -> assertThat(p.get("category").asText()).isEqualTo("Books"));
    }

    @Test
    void searchTreatsLikeWildcardsLiterally() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidPriceRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/products").param("minPrice", "100").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("minPrice must not be greater than maxPrice"));
        mockMvc.perform(get("/api/products").param("minPrice", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownProductReturns404() throws Exception {
        mockMvc.perform(get("/api/products/999999")).andExpect(status().isNotFound());
    }

    @Test
    void regularUserCannotModifyProducts() throws Exception {
        Long id = productRepository.findAll().get(0).getId();
        String body = objectMapper.writeValueAsString(
                new ProductRequest("Hacked", "Books", BigDecimal.ONE, 1, 1.0));

        mockMvc.perform(put("/api/products/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/products/" + id)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreateValidatesInput() throws Exception {
        String body = objectMapper.writeValueAsString(new ProductRequest("", "Books", new BigDecimal("-1"), -5, 7.0));
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    private JsonNode content(MockHttpServletRequestBuilder request) throws Exception {
        String json = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("content");
    }
}
