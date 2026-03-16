package com.monat.ecommerce.product.application.query;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * CQRS Query: Ürün arama isteğini temsil eder.
 * <p>
 * Query Pattern: Sistemi değiştirmeyen, sadece veri okuyan işlemleri kapsüller.
 * Immutable record olduğu için thread-safe ve cache key olarak kullanılabilir.
 * <p>
 * Elasticsearch üzerinden arama yapılır; circuit breaker ile korunur.
 * </p>
 */
public record SearchProductsQuery(
        String keyword,
        String category,
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable) {
}
