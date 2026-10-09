package com.edstem.interviewprep.product;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductFilter(
        @Size(max = 60, message = "Category must be at most 60 characters")
        String category,

        @PositiveOrZero(message = "minPrice must be zero or positive")
        BigDecimal minPrice,

        @PositiveOrZero(message = "maxPrice must be zero or positive")
        BigDecimal maxPrice,

        Boolean inStock,

        @Size(max = 100, message = "Search text must be at most 100 characters")
        String q) {

    @AssertTrue(message = "minPrice must not be greater than maxPrice")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }
}
