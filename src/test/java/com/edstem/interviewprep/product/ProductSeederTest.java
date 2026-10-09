package com.edstem.interviewprep.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProductSeederTest {

    @Test
    void generatesExactly100ValidProducts() {
        List<Product> products = ProductSeeder.generate(ProductSeeder.PRODUCT_COUNT);

        assertThat(products).hasSize(100);
        assertThat(products).allSatisfy(p -> {
            assertThat(p.getName()).isNotBlank();
            assertThat(p.getPrice()).isPositive();
            assertThat(p.getStock()).isNotNegative();
            assertThat(p.getRating()).isBetween(0.0, 5.0);
        });
        assertThat(products).anyMatch(p -> p.getStock() == 0);
        assertThat(products).extracting(Product::getCategory).hasSizeGreaterThan(1);
    }

    @Test
    void seedDataIsDeterministic() {
        assertThat(ProductSeeder.generate(5)).extracting(Product::getName)
                .isEqualTo(ProductSeeder.generate(5).stream().map(Product::getName).toList());
    }
}
