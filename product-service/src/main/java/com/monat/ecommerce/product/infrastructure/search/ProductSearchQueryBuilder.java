package com.monat.ecommerce.product.infrastructure.search;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Elasticsearch NativeQuery Builder.
 * <p>
 * Builder Pattern: Constructs complex Elasticsearch queries step-by-step.
 * <p>
 * Why NativeQuery / CriteriaQuery?
 * - Spring Data's @Query annotation uses static JSON strings.
 * - For dynamic filters (optional keyword, category, price range), a programmatic query builder 
 *   is much cleaner and type-safe.
 * - CriteriaQuery expresses the Elasticsearch DSL via a Java API.
 * <p>
 * Query Strategy:
 * - If keyword is present: multi-match across name^3, description^2, category, and brand fields.
 * - If category is present: exact match (Keyword field).
 * - If brand is present: exact match (Keyword field).
 * - If price range is present: range filter.
 * All criteria are combined using AND logic.
 */
@Component
@RequiredArgsConstructor
public class ProductSearchQueryBuilder {

    /**
     * Constructs a dynamic Elasticsearch query from the given parameters.
     * Null parameters are excluded from the query (optional filter pattern).
     *
     * @param keyword  Full-text search term (matches name, description, category, brand)
     * @param category Category filter (exact match)
     * @param brand    Brand filter (exact match)
     * @param minPrice Minimum price (inclusive)
     * @param maxPrice Maximum price (inclusive)
     * @param pageable Pagination and sorting information
     * @return Query object to be sent to Elasticsearch
     */
    public Query buildSearchQuery(
            String keyword,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        // Criteria: A chain of filters to be combined with AND logic
        Criteria criteria = new Criteria();
        boolean hasCriteria = false;

        // Full-text search: Search multiple fields with boosting
        if (keyword != null && !keyword.isBlank()) {
            // Apply 3x boost to name field and 2x boost to description
            Criteria keywordCriteria = new Criteria("name").boost(3f).matches(keyword)
                    .or(new Criteria("description").boost(2f).matches(keyword))
                    .or(new Criteria("category").matches(keyword))
                    .or(new Criteria("brand").matches(keyword));
            criteria = hasCriteria ? criteria.and(keywordCriteria) : keywordCriteria;
            hasCriteria = true;
        }

        // Category filter: Keyword field — exact match
        if (category != null && !category.isBlank()) {
            Criteria categoryCriteria = new Criteria("category").is(category);
            criteria = hasCriteria ? criteria.and(categoryCriteria) : categoryCriteria;
            hasCriteria = true;
        }

        // Brand filter: Keyword field — exact match
        if (brand != null && !brand.isBlank()) {
            Criteria brandCriteria = new Criteria("brand").is(brand);
            criteria = hasCriteria ? criteria.and(brandCriteria) : brandCriteria;
            hasCriteria = true;
        }

        // Price range filter: range query
        if (minPrice != null && maxPrice != null) {
            Criteria priceCriteria = new Criteria("price").greaterThanEqual(minPrice).lessThanEqual(maxPrice);
            criteria = hasCriteria ? criteria.and(priceCriteria) : priceCriteria;
            hasCriteria = true;
        } else if (minPrice != null) {
            Criteria priceCriteria = new Criteria("price").greaterThanEqual(minPrice);
            criteria = hasCriteria ? criteria.and(priceCriteria) : priceCriteria;
            hasCriteria = true;
        } else if (maxPrice != null) {
            Criteria priceCriteria = new Criteria("price").lessThanEqual(maxPrice);
            criteria = hasCriteria ? criteria.and(priceCriteria) : priceCriteria;
            hasCriteria = true;
        }

        // If no filters are provided, default to a match_all query (returning all active products)
        if (!hasCriteria) {
            criteria = new Criteria("status").is("ACTIVE");
        }

        return new CriteriaQuery(criteria, pageable);
    }
}
