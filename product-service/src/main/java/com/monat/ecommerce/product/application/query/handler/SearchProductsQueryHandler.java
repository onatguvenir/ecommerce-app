package com.monat.ecommerce.product.application.query.handler;

import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.mapper.ProductMapper;
import com.monat.ecommerce.product.application.query.SearchProductsQuery;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchDocument;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchQueryBuilder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SearchProductsQuery Handler.
 * <p>
 * CQRS Read Side: Advanced product search via Elasticsearch.
 * <p>
 * Resilience4j Circuit Breaker:
 * - If Elasticsearch is unavailable, the circuit opens.
 * - Fallback: Executes a simplified search query via MongoDB.
 * - This ensures that an Elasticsearch outage doesn't completely disable the search feature.
 * <p>
 * Circuit Breaker States:
 * - CLOSED: Normal operation, requests routed to ES.
 * - OPEN: ES is failing, requests routed to fallback.
 * - HALF_OPEN: Test requests are sent; if ES has recovered, state returns to CLOSED.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchProductsQueryHandler implements QueryHandler<SearchProductsQuery, Page<ProductResponse>> {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchQueryBuilder queryBuilder;
    private final ProductRepository productRepository; // For fallback
    private final ProductMapper productMapper;

    /**
     * @CircuitBreaker: Uses the circuit breaker named "elasticsearchCB".
     *                  fallbackMethod: Method invoked when ES is unreachable.
     *                  Configured in application.yml under resilience4j.circuitbreaker.instances.elasticsearchCB.
     */
    @Override
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "searchFallback")
    public Page<ProductResponse> handle(SearchProductsQuery query) {
        log.debug("Searching products in Elasticsearch — keyword: {}, category: {}",
                query.keyword(), query.category());

        // Construct dynamic query using NativeQuery builder
        Query nativeQuery = queryBuilder.buildSearchQuery(
                query.keyword(),
                query.category(),
                query.brand(),
                query.minPrice(),
                query.maxPrice(),
                query.pageable());

        SearchHits<ProductSearchDocument> searchHits = elasticsearchOperations.search(nativeQuery,
                ProductSearchDocument.class);

        List<ProductResponse> results = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(productMapper::toResponse)
                .toList();

        long totalHits = searchHits.getTotalHits();

        log.debug("Elasticsearch returned {} hits", totalHits);

        return new PageImpl<>(results, query.pageable(), totalHits);
    }

    /**
     * Circuit Breaker Fallback: Falls back to MongoDB when Elasticsearch is unavailable.
     * <p>
     * Fallback method signature: matches original method + Throwable parameter.
     * This allows logging the specific exception that triggered the circuit.
     * </p>
     */
    @SuppressWarnings("unused")
    public Page<ProductResponse> searchFallback(SearchProductsQuery query, Throwable throwable) {
        log.warn("Elasticsearch circuit breaker OPEN — falling back to MongoDB. Reason: {}",
                throwable.getMessage());

        // Basic category/keyword search via MongoDB
        if (query.category() != null) {
            return productRepository.findByCategory(query.category(), query.pageable())
                    .map(productMapper::toResponse);
        }

        if (query.keyword() != null) {
            return productRepository.findByNameContainingIgnoreCase(query.keyword(), query.pageable())
                    .map(productMapper::toResponse);
        }

        return productRepository.findByStatus(ProductStatus.ACTIVE, query.pageable())
                    .map(productMapper::toResponse);
    }
}
