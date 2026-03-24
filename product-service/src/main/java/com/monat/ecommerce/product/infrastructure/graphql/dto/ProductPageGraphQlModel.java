package com.monat.ecommerce.product.infrastructure.graphql.dto;

import java.util.List;

public record ProductPageGraphQlModel(
        List<ProductGraphQlModel> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {
}
