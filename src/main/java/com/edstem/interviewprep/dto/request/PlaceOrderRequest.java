package com.edstem.interviewprep.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty(message = "An order needs at least one item")
        @Size(max = 50, message = "An order can have at most 50 items")
        List<@Valid @NotNull(message = "Item must not be null") Item> items) {

    public record Item(
            @NotNull(message = "productId is required")
            Long productId,

            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be at least 1")
            @Max(value = 1000, message = "quantity must be at most 1000")
            Integer quantity) {
    }
}
