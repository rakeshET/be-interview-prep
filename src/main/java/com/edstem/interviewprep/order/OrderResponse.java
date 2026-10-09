package com.edstem.interviewprep.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        String customer,
        List<Line> items,
        BigDecimal total,
        Instant createdAt) {

    public record Line(Long productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    static OrderResponse from(Order order) {
        List<Line> lines = order.getItems().stream()
                .map(i -> new Line(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(),
                        i.getLineTotal()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getCustomer(), lines, order.getTotal(),
                order.getCreatedAt());
    }
}
