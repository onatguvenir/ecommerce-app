package com.monat.ecommerce.product.application.query;

/**
 * CQRS Query: Tekil ürün sorgusunu temsil eder.
 * <p>
 * productId ile MongoDB veya Redis cache'den ürün getirir.
 * </p>
 */
public record GetProductQuery(String productId) {
}
