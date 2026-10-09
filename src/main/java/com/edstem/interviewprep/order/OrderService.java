package com.edstem.interviewprep.order;

import com.edstem.interviewprep.common.error.BadRequestException;
import com.edstem.interviewprep.common.error.NotFoundException;
import com.edstem.interviewprep.product.Product;
import com.edstem.interviewprep.product.ProductCacheConfig;
import com.edstem.interviewprep.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;
    private final Cache productCache;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        TransactionTemplate transactionTemplate, CacheManager cacheManager) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.transactionTemplate = transactionTemplate;
        this.productCache = cacheManager.getCache(ProductCacheConfig.PRODUCTS_CACHE);
    }

    public PlacementResult place(String customer, String idempotencyKey, PlaceOrderRequest request) {
        SortedMap<Long, Integer> quantities = mergeByProduct(request);
        String requestHash = hash(quantities);

        Optional<PlacementResult> replay = findReplay(customer, idempotencyKey, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }
        try {
            Order order = transactionTemplate.execute(
                    status -> reserveStockAndSave(customer, idempotencyKey, requestHash, quantities));
            return new PlacementResult(OrderResponse.from(order), true);
        } catch (DataIntegrityViolationException e) {
            return findReplay(customer, idempotencyKey, requestHash).orElseThrow(() -> e);
        }
    }

    @Transactional
    public OrderResponse cancel(Long orderId, String customer, boolean admin) {
        Order order = findVisible(orderId, customer, admin);
        if (orderRepository.markCancelled(orderId) == 1) {
            for (OrderItem item : order.getItems()) {
                productRepository.releaseStock(item.getProductId(), item.getQuantity());
                productCache.evict(item.getProductId());
            }
        }
        return OrderResponse.from(findVisible(orderId, customer, admin));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long orderId, String customer, boolean admin) {
        return OrderResponse.from(findVisible(orderId, customer, admin));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMine(String customer) {
        return orderRepository.findByCustomerOrderByIdDesc(customer).stream().map(OrderResponse::from).toList();
    }

    private Order reserveStockAndSave(String customer, String idempotencyKey, String requestHash,
                                      SortedMap<Long, Integer> quantities) {
        Order order = new Order(customer, idempotencyKey, requestHash);
        for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
            Long productId = line.getKey();
            int quantity = line.getValue();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product", productId));
            if (productRepository.reserveStock(productId, quantity) == 0) {
                throw new InsufficientStockException(productId, product.getName(), quantity);
            }
            order.addItem(productId, product.getName(), product.getPrice(), quantity);
            productCache.evict(productId);
        }
        return orderRepository.saveAndFlush(order);
    }

    private Optional<PlacementResult> findReplay(String customer, String idempotencyKey, String requestHash) {
        return orderRepository.findByCustomerAndIdempotencyKey(customer, idempotencyKey).map(existing -> {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new BadRequestException("Idempotency-Key",
                        "This Idempotency-Key was already used for a different order request");
            }
            return new PlacementResult(OrderResponse.from(existing), false);
        });
    }

    private Order findVisible(Long orderId, String customer, boolean admin) {
        return orderRepository.findWithItemsById(orderId)
                .filter(order -> admin || order.getCustomer().equals(customer))
                .orElseThrow(() -> new NotFoundException("Order", orderId));
    }

    private static SortedMap<Long, Integer> mergeByProduct(PlaceOrderRequest request) {
        return request.items().stream().collect(Collectors.toMap(
                PlaceOrderRequest.Item::productId, PlaceOrderRequest.Item::quantity, Integer::sum, TreeMap::new));
    }

    private static String hash(SortedMap<Long, Integer> quantities) {
        String canonical = quantities.entrySet().stream()
                .map(e -> e.getKey() + "x" + e.getValue())
                .collect(Collectors.joining(","));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record PlacementResult(OrderResponse order, boolean created) {
    }
}
