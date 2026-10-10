package com.edstem.interviewprep.service;

import com.edstem.interviewprep.config.ProductCacheConfig;
import com.edstem.interviewprep.dto.request.ProductFilter;
import com.edstem.interviewprep.dto.request.ProductRequest;
import com.edstem.interviewprep.dto.response.PageResponse;
import com.edstem.interviewprep.dto.response.ProductResponse;
import com.edstem.interviewprep.entity.Order;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.exception.BadRequestException;
import com.edstem.interviewprep.exception.NotFoundException;
import com.edstem.interviewprep.repository.ProductRepository;
import com.edstem.interviewprep.repository.ProductSpecifications;

import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    static final int MAX_PAGE_SIZE = 100;
    static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "category", "price", "stock", "rating",
            "createdAt");

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public PageResponse<ProductResponse> list(ProductFilter filter, Pageable pageable) {
        Pageable safe = sanitize(pageable);
        return PageResponse.from(productRepository.findAll(ProductSpecifications.matching(filter), safe)
                .map(ProductResponse::from));
    }

    @Cacheable(cacheNames = ProductCacheConfig.PRODUCTS_CACHE, key = "#id")
    public ProductResponse get(Long id) {
        return ProductResponse.from(findOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.name().trim(), request.category().trim(), request.price(),
                request.stock(), request.rating());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(cacheNames = ProductCacheConfig.PRODUCTS_CACHE, key = "#id")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        product.update(request.name().trim(), request.category().trim(), request.price(), request.stock(),
                request.rating());
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = ProductCacheConfig.PRODUCTS_CACHE, key = "#id")
    public void delete(Long id) {
        productRepository.delete(findOrThrow(id));
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }

    private static Pageable sanitize(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("sort", "Cannot sort by '" + order.getProperty()
                        + "'. Allowed: " + SORTABLE_FIELDS.stream().sorted().toList());
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("id");
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
