package com.monat.ecommerce.product.application.query.handler;

import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.mapper.ProductMapper;
import com.monat.ecommerce.product.application.query.SearchProductsQuery;
import com.monat.ecommerce.product.domain.model.Product;
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
 * CQRS Read Side: Elasticsearch üzerinden gelişmiş ürün araması.
 * <p>
 * Resilience4j Circuit Breaker:
 * - Elasticsearch erişilemez olduğunda circuit açılır.
 * - Fallback: MongoDB üzerinden basit arama yapılır.
 * - Bu sayede Elasticsearch arızası tüm arama özelliğini çökertmez.
 * <p>
 * Circuit Breaker States:
 * - CLOSED: Normal çalışma, ES'e gider
 * - OPEN: ES arızalı, fallback devreye girer
 * - HALF_OPEN: Test istekleri gönderilir, ES iyileştiyse CLOSED'a döner
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchProductsQueryHandler implements QueryHandler<SearchProductsQuery, Page<ProductResponse>> {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchQueryBuilder queryBuilder;
    private final ProductRepository productRepository; // Fallback için
    private final ProductMapper productMapper;

    /**
     * @CircuitBreaker: "elasticsearchCB" adlı circuit breaker'ı kullanır.
     *                  fallbackMethod: ES erişilemez olduğunda çağrılacak metod.
     *                  application.yml'de
     *                  resilience4j.circuitbreaker.instances.elasticsearchCB ile
     *                  yapılandırılır.
     */
    @Override
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "searchFallback")
    public Page<ProductResponse> handle(SearchProductsQuery query) {
        log.debug("Searching products in Elasticsearch — keyword: {}, category: {}",
                query.keyword(), query.category());

        // NativeQuery builder ile dinamik sorgu oluştur
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
     * Circuit Breaker Fallback: Elasticsearch erişilemez olduğunda MongoDB'ye
     * düşer.
     * <p>
     * Fallback metod imzası: orijinal metod + Throwable parametresi.
     * Bu sayede hangi exception'ın circuit'i açtığı loglanabilir.
     * </p>
     */
    @SuppressWarnings("unused")
    public Page<ProductResponse> searchFallback(SearchProductsQuery query, Throwable throwable) {
        log.warn("Elasticsearch circuit breaker OPEN — falling back to MongoDB. Reason: {}",
                throwable.getMessage());

        // MongoDB üzerinden basit kategori/isim araması
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
