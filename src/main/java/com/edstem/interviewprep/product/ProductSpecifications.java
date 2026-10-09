package com.edstem.interviewprep.product;

import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Each filter is an independent predicate that is skipped when its value is absent, so they can be
 * combined freely into one WHERE clause - no query method per combination.
 */
final class ProductSpecifications {

    private ProductSpecifications() {
    }

    static Specification<Product> matching(ProductFilter filter) {
        return Specification.allOf(
                categoryEquals(filter.category()),
                priceAtLeast(filter),
                priceAtMost(filter),
                inStockOnly(filter.inStock()),
                nameContains(filter.q()));
    }

    private static Specification<Product> categoryEquals(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase(Locale.ROOT));
    }

    private static Specification<Product> priceAtLeast(ProductFilter filter) {
        return filter.minPrice() == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice());
    }

    private static Specification<Product> priceAtMost(ProductFilter filter) {
        return filter.maxPrice() == null ? null
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice());
    }

    private static Specification<Product> inStockOnly(Boolean inStock) {
        return Boolean.TRUE.equals(inStock) ? (root, query, cb) -> cb.greaterThan(root.get("stock"), 0) : null;
    }

    private static Specification<Product> nameContains(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        // Escape LIKE wildcards so a search for "50%" means the literal text, not "starts with 50".
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\');
    }
}
