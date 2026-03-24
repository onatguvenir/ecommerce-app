package com.monat.ecommerce.product.application.query;

import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.mapper.ProductMapper;
import com.monat.ecommerce.product.application.query.handler.GetProductQueryHandler;
import com.monat.ecommerce.product.application.query.handler.SearchProductsQueryHandler;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CQRS Query Service — Read Side Facade.
 * <p>
 * Orchestrates all read operations.
 * The read model is entirely independent of the write model:
 * - Single Lookup: Redis cache → MongoDB fallback.
 * - Search: Elasticsearch (with circuit breaker) → MongoDB fallback.
 * - Listing: MongoDB (paginated).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final GetProductQueryHandler getProductHandler;
    private final SearchProductsQueryHandler searchHandler;
    private final ProductRepository productRepository; // Liste sorguları için
    private final ProductMapper productMapper;

    public ProductResponse getProduct(String productId) {
        return getProductHandler.handle(new GetProductQuery(productId));
    }

    public Page<ProductResponse> searchProducts(SearchProductsQuery query) {
        return searchHandler.handle(query);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable).map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByStatus(ProductStatus status, Pageable pageable) {
        return productRepository.findByStatus(status, pageable).map(productMapper::toResponse);
    }
}
