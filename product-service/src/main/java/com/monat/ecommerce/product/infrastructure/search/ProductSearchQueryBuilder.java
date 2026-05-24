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
 * - keyword: name match^3 + name prefix^2 + description match^1.5 + category wildcard + brand wildcard
 * - category filter: exact term (Keyword field)
 * - brand filter: exact term (Keyword field)
 * - price range: range filter
 * All criteria combined with AND logic.
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

        if (keyword != null && !keyword.isBlank()) {
            // name (Text): match = full token hit; startsWith = prefix query on analyzed token
            // description (Text): match only — prefix on long text produces noise
            // category/brand (Keyword): contains = wildcard *kw* — case-sensitive but handles substrings;
            //   matches would generate a case-sensitive term query with no substring support
            Criteria keywordCriteria = new Criteria("name").boost(3f).matches(keyword)
                    .or(new Criteria("name").boost(2f).startsWith(keyword))
                    .or(new Criteria("description").boost(1.5f).matches(keyword))
                    .or(new Criteria("category").matches(keyword))
                    .or(new Criteria("category").startsWith(keyword))
                    .or(new Criteria("brand").matches(keyword))
                    .or(new Criteria("brand").startsWith(keyword));
            criteria = hasCriteria ? criteria.and(keywordCriteria) : keywordCriteria;
            hasCriteria = true;
        }

        // Category filter: .keyword subfield — exact match (MultiField Keyword subfield)
        if (category != null && !category.isBlank()) {
            Criteria categoryCriteria = new Criteria("category.keyword").is(category);
            criteria = hasCriteria ? criteria.and(categoryCriteria) : categoryCriteria;
            hasCriteria = true;
        }

        // Brand filter: .keyword subfield — exact match (MultiField Keyword subfield)
        if (brand != null && !brand.isBlank()) {
            Criteria brandCriteria = new Criteria("brand.keyword").is(brand);
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
