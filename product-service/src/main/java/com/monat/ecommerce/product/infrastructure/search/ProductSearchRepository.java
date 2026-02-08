package com.monat.ecommerce.product.infrastructure.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Elasticsearch repository for full-text product search
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, String> {

    Page<ProductSearchDocument> findByName(String name, Pageable pageable);

    Page<ProductSearchDocument> findByCategory(String category, Pageable pageable);

    Page<ProductSearchDocument> findByBrand(String brand, Pageable pageable);

    Page<ProductSearchDocument> findByTagsContaining(String tag, Pageable pageable);

    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name^3\", \"description^2\", \"category\", \"brand\"]}}]}}")
    Page<ProductSearchDocument> searchByKeyword(String keyword, Pageable pageable);

    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name^3\", \"description^2\"]}}, {\"range\": {\"price\": {\"gte\": ?1, \"lte\": ?2}}}]}}")
    Page<ProductSearchDocument> searchByKeywordAndPriceRange(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"category\": \"?0\"}}, {\"range\": {\"price\": {\"gte\": ?1, \"lte\": ?2}}}]}}")
    Page<ProductSearchDocument> findByCategoryAndPriceBetween(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
