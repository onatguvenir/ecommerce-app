package com.monat.ecommerce.product.application.query;

import org.springframework.data.domain.Pageable;

/**
 * CQRS Query: Kategori bazlı ürün listesi sorgusunu temsil eder.
 */
public record GetProductsByCategoryQuery(String category, Pageable pageable) {
}
