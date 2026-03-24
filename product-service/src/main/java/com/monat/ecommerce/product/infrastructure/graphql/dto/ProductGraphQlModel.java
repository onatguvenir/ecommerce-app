package com.monat.ecommerce.product.infrastructure.graphql.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductGraphQlModel(
        String id,
        String productId,
        String name,
        String description,
        String category,
        String brand,
        BigDecimal price,
        String currency,
        List<String> images,
        List<String> tags,
        String status,
        String weight,
        String dimensions,
        String color,
        String material,
        List<ProductSpecificationEntry> additionalSpecs) {
}
