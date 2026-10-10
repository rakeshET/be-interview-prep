package com.edstem.interviewprep.controller;

import com.edstem.interviewprep.dto.request.PlaceOrderRequest;
import com.edstem.interviewprep.entity.Order;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.repository.OrderRepository;
import com.edstem.interviewprep.repository.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "alice@example.com")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long pen;
    private Long notebook;

    @BeforeEach
    void createProducts() {
        pen = productRepository.save(new Product("Order Test Pen", "Office", new BigDecimal("2.50"), 10, 4.0)).getId();
        notebook = productRepository.save(new Product("Order Test Notebook", "Office", new BigDecimal("5.00"), 1, 4.2))
                .getId();
    }

    @Test
    void placingAnOrderReservesStockAndReturns201() throws Exception {
        place(UUID.randomUUID().toString(), item(pen, 3), item(notebook, 1))
                .andExpect(status().isCreated())
                .andExpect(header().string(OrderController.REPLAYED_HEADER, "false"))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.customer").value("alice@example.com"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(12.50));

        assertThat(stockOf(pen)).isEqualTo(7);
        assertThat(stockOf(notebook)).isZero();
    }

    @Test
    void retryingTheSameRequestCreatesOnlyOneOrder() throws Exception {
        String key = UUID.randomUUID().toString();
        long ordersBefore = orderRepository.count();

        String first = place(key, item(pen, 2)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String retry = place(key, item(pen, 2)).andExpect(status().isOk())
                .andExpect(header().string(OrderController.REPLAYED_HEADER, "true"))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(retry).get("id")).isEqualTo(objectMapper.readTree(first).get("id"));
        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
        assertThat(stockOf(pen)).isEqualTo(8);
    }

    @Test
    void reusingAKeyForADifferentRequestIsRejected() throws Exception {
        String key = UUID.randomUUID().toString();
        place(key, item(pen, 1)).andExpect(status().isCreated());

        place(key, item(pen, 5))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("Idempotency-Key"));
        assertThat(stockOf(pen)).isEqualTo(9);
    }

    @Test
    void theSameKeyFromAnotherCustomerIsANewOrder() throws Exception {
        String key = UUID.randomUUID().toString();
        place(key, item(pen, 1)).andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders").with(user("bob@example.com"))
                        .header(OrderController.IDEMPOTENCY_KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(item(pen, 1))))
                .andExpect(status().isCreated());
        assertThat(stockOf(pen)).isEqualTo(8);
    }

    @Test
    void insufficientStockReturns409AndReservesNothing() throws Exception {
        long ordersBefore = orderRepository.count();

        place(UUID.randomUUID().toString(), item(pen, 2), item(notebook, 5))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString(
                        "Insufficient stock for product 'Order Test Notebook'")));

        assertThat(stockOf(pen)).isEqualTo(10);
        assertThat(stockOf(notebook)).isEqualTo(1);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }

    @Test
    void unknownProductReturns404AndReservesNothing() throws Exception {
        place(UUID.randomUUID().toString(), item(pen, 1), item(999_999L, 1))
                .andExpect(status().isNotFound());
        assertThat(stockOf(pen)).isEqualTo(10);
    }

    @Test
    void duplicateLinesForTheSameProductAreMerged() throws Exception {
        place(UUID.randomUUID().toString(), item(pen, 2), item(pen, 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
        assertThat(stockOf(pen)).isEqualTo(5);
    }

    @Test
    void cancellingReturnsStockExactlyOnce() throws Exception {
        long orderId = placedOrderId(item(pen, 4), item(notebook, 1));
        assertThat(stockOf(pen)).isEqualTo(6);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(stockOf(pen)).isEqualTo(10);
        assertThat(stockOf(notebook)).isEqualTo(1);
    }

    @Test
    void otherCustomersCannotSeeOrCancelMyOrderButAdminsCan() throws Exception {
        long orderId = placedOrderId(item(pen, 1));

        mockMvc.perform(get("/api/orders/" + orderId).with(user("bob@example.com")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel").with(user("bob@example.com")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders/" + orderId).with(user("root@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
        assertThat(stockOf(pen)).isEqualTo(9);
    }

    @Test
    void productLookupShowsNewStockAfterAnOrder() throws Exception {
        mockMvc.perform(get("/api/products/" + pen)).andExpect(jsonPath("$.stock").value(10));

        placedOrderId(item(pen, 4));

        mockMvc.perform(get("/api/products/" + pen)).andExpect(jsonPath("$.stock").value(6));
    }

    @Test
    void missingIdempotencyKeyReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body(item(pen, 1))))
                .andExpect(status().isBadRequest());
        assertThat(stockOf(pen)).isEqualTo(10);
    }

    @Test
    void invalidOrderBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/orders").header(OrderController.IDEMPOTENCY_KEY_HEADER, "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items"));
        place(UUID.randomUUID().toString(), item(pen, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].quantity"));
    }

    @Test
    void listsOnlyMyOrders() throws Exception {
        placedOrderId(item(pen, 1));
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].customer").value(everyItem(is("alice@example.com"))));
    }

    private long placedOrderId(PlaceOrderRequest.Item... items) throws Exception {
        String response = place(UUID.randomUUID().toString(), items).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private ResultActions place(String key, PlaceOrderRequest.Item... items) throws Exception {
        return mockMvc.perform(post("/api/orders")
                .header(OrderController.IDEMPOTENCY_KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(items)));
    }

    private String body(PlaceOrderRequest.Item... items) throws Exception {
        return objectMapper.writeValueAsString(new PlaceOrderRequest(List.of(items)));
    }

    private static PlaceOrderRequest.Item item(Long productId, int quantity) {
        return new PlaceOrderRequest.Item(productId, quantity);
    }

    private int stockOf(Long productId) {
        return productRepository.findById(productId).orElseThrow().getStock();
    }
}
