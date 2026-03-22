package com.monat.ecommerce.product.application.query.handler;

import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.mapper.ProductMapper;
import com.monat.ecommerce.product.application.query.SearchProductsQuery;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchDocument;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchQueryBuilder;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SearchProductsQueryHandler Unit Tests.
 * <p>
 * Test Stratejisi:
 * - Normal path: Elasticsearch başarılı sonuç döner.
 * - Fallback path: ES exception fırlatır, MongoDB fallback devreye girer.
 * - Circuit breaker annotation'ı unit test'te aktif değil (Spring context yok),
 * bu yüzden fallback metodu doğrudan test edilir.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchProductsQueryHandler Tests")
class SearchProductsQueryHandlerTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ProductSearchQueryBuilder queryBuilder;

    @Mock
    private ProductRepository productRepository;

    private SearchProductsQueryHandler handler;
    private ProductMapper productMapper;

    private PageRequest pageable;
    private ProductSearchDocument searchDocument;
    private Product fallbackProduct;

    @BeforeEach
    void setUp() {
        productMapper = Mappers.getMapper(ProductMapper.class);
        handler = new SearchProductsQueryHandler(elasticsearchOperations, queryBuilder, productRepository, productMapper);

        pageable = PageRequest.of(0, 10);

        searchDocument = ProductSearchDocument.builder()
                .id("mongo-id-001")
                .productId("PROD-001")
                .name("Test Product")
                .description("A test product")
                .category("Electronics")
                .brand("TestBrand")
                .price(BigDecimal.valueOf(99.99))
                .tags(List.of("tag1"))
                .status("ACTIVE")
                .build();

        fallbackProduct = Product.builder()
                .id("mongo-id-001")
                .productId("PROD-001")
                .name("Test Product")
                .category("Electronics")
                .price(BigDecimal.valueOf(99.99))
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Elasticsearch başarılı sonuç döndüğünde ürünler listelenmeli")
    @SuppressWarnings("unchecked")
    void handle_ElasticsearchSuccess_ShouldReturnMappedResults() {
        // Given
        SearchProductsQuery query = new SearchProductsQuery("test", null, null, null, null, pageable);

        Query mockQuery = mock(Query.class);
        when(queryBuilder.buildSearchQuery(any(), any(), any(), any(), any(), any()))
                .thenReturn(mockQuery);

        SearchHit<ProductSearchDocument> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(searchDocument);

        SearchHits<ProductSearchDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(searchHits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(Query.class), eq(ProductSearchDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductResponse> result = handler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productId()).isEqualTo("PROD-001");
        assertThat(result.getContent().get(0).name()).isEqualTo("Test Product");

        verify(elasticsearchOperations).search(any(Query.class), eq(ProductSearchDocument.class));
        verifyNoInteractions(productRepository); // Fallback devreye girmemeli
    }

    @Test
    @DisplayName("Fallback: category varsa MongoDB'den kategori araması yapılmalı")
    void searchFallback_WithCategory_ShouldQueryMongoByCategory() {
        // Given
        SearchProductsQuery query = new SearchProductsQuery(null, "Electronics", null, null, null, pageable);
        RuntimeException esException = new RuntimeException("Elasticsearch unavailable");

        Page<Product> mongoPage = new PageImpl<>(List.of(fallbackProduct));
        when(productRepository.findByCategory("Electronics", pageable)).thenReturn(mongoPage);

        // When — fallback metodu doğrudan çağrılır (circuit breaker Spring context'te
        // aktif)
        Page<ProductResponse> result = handler.searchFallback(query, esException);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).category()).isEqualTo("Electronics");

        verify(productRepository).findByCategory("Electronics", pageable);
        verify(productRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("Fallback: keyword varsa MongoDB'den isim araması yapılmalı")
    void searchFallback_WithKeyword_ShouldQueryMongoByName() {
        // Given
        SearchProductsQuery query = new SearchProductsQuery("laptop", null, null, null, null, pageable);
        RuntimeException esException = new RuntimeException("Elasticsearch timeout");

        Page<Product> mongoPage = new PageImpl<>(List.of(fallbackProduct));
        when(productRepository.findByNameContainingIgnoreCase("laptop", pageable)).thenReturn(mongoPage);

        // When
        Page<ProductResponse> result = handler.searchFallback(query, esException);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(productRepository).findByNameContainingIgnoreCase("laptop", pageable);
    }

    @Test
    @DisplayName("Fallback: filtre yoksa aktif ürünler listelenmeli")
    void searchFallback_NoFilters_ShouldReturnActiveProducts() {
        // Given
        SearchProductsQuery query = new SearchProductsQuery(null, null, null, null, null, pageable);
        RuntimeException esException = new RuntimeException("Circuit open");

        Page<Product> mongoPage = new PageImpl<>(List.of(fallbackProduct));
        when(productRepository.findByStatus(ProductStatus.ACTIVE, pageable)).thenReturn(mongoPage);

        // When
        Page<ProductResponse> result = handler.searchFallback(query, esException);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(productRepository).findByStatus(ProductStatus.ACTIVE, pageable);
    }
}
