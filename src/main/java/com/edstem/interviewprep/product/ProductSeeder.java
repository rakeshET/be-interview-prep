package com.edstem.interviewprep.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds 100 products on startup when the table is empty. A fixed random seed makes the data
 * identical on every run, so demos and manual tests are reproducible.
 */
@Component
@ConditionalOnProperty(name = "app.catalog.seed", havingValue = "true", matchIfMissing = true)
public class ProductSeeder implements ApplicationRunner {

    static final int PRODUCT_COUNT = 100;
    private static final Logger log = LoggerFactory.getLogger(ProductSeeder.class);
    private static final List<String> CATEGORIES = List.of("Electronics", "Books", "Home", "Sports", "Toys");
    private static final List<String> ADJECTIVES = List.of("Classic", "Smart", "Compact", "Deluxe", "Eco", "Ultra",
            "Portable", "Vintage", "Pro", "Mini");
    private static final List<String> NOUNS = List.of("Lamp", "Speaker", "Notebook", "Backpack", "Bottle", "Puzzle",
            "Headphones", "Chair", "Ball", "Watch");

    private final ProductRepository productRepository;

    public ProductSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }
        productRepository.saveAll(generate(PRODUCT_COUNT));
        log.info("Seeded {} products", PRODUCT_COUNT);
    }

    static List<Product> generate(int count) {
        Random random = new Random(42);
        List<Product> products = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String name = ADJECTIVES.get(random.nextInt(ADJECTIVES.size())) + " "
                    + NOUNS.get(random.nextInt(NOUNS.size())) + " " + i;
            String category = CATEGORIES.get(random.nextInt(CATEGORIES.size()));
            BigDecimal price = BigDecimal.valueOf(5 + random.nextDouble() * 495).setScale(2, RoundingMode.HALF_UP);
            int stock = random.nextInt(5) == 0 ? 0 : random.nextInt(200); // ~20% out of stock
            double rating = Math.round((1 + random.nextDouble() * 4) * 10) / 10.0;
            products.add(new Product(name, category, price, stock, rating));
        }
        return products;
    }
}
