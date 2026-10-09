package com.edstem.interviewprep.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.product.Product;
import com.edstem.interviewprep.product.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class OrderConcurrencyTest {

    private static final int CUSTOMERS = 50;
    private static final int STOCK = 10;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void fiftySimultaneousOrdersForStockOfTenExactlyTenSucceed() throws Exception {
        Long productId = productRepository.save(new Product("Limited Sneaker", "Sports", new BigDecimal("99.00"),
                STOCK, 4.9)).getId();
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        runSimultaneously(CUSTOMERS, i -> () -> {
            try {
                orderService.place("customer" + i + "@example.com", UUID.randomUUID().toString(),
                        new PlaceOrderRequest(List.of(new PlaceOrderRequest.Item(productId, 1))));
                succeeded.incrementAndGet();
            } catch (InsufficientStockException e) {
                rejected.incrementAndGet();
            }
            return null;
        });

        assertThat(succeeded.get()).isEqualTo(STOCK);
        assertThat(rejected.get()).isEqualTo(CUSTOMERS - STOCK);
        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isZero();
        Long itemsOrdered = transactionTemplate.execute(status -> orderRepository.findAll().stream()
                .flatMap(o -> o.getItems().stream())
                .filter(item -> item.getProductId().equals(productId))
                .count());
        assertThat(itemsOrdered).isEqualTo(STOCK);
    }

    @Test
    void simultaneousRetriesWithTheSameKeyCreateOneOrder() throws Exception {
        Long productId = productRepository.save(new Product("Retry Mug", "Home", new BigDecimal("12.00"), 100, 4.0))
                .getId();
        String key = UUID.randomUUID().toString();
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(new PlaceOrderRequest.Item(productId, 2)));
        List<Long> orderIds = Collections.synchronizedList(new ArrayList<>());

        runSimultaneously(10, i -> () -> {
            orderIds.add(orderService.place("retry@example.com", key, request).order().id());
            return null;
        });

        assertThat(orderIds).hasSize(10).containsOnly(orderIds.get(0));
        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isEqualTo(98);
    }

    private static void runSimultaneously(int tasks, IntFunction<Callable<Void>> taskFactory)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < tasks; i++) {
                Callable<Void> task = taskFactory.apply(i);
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
