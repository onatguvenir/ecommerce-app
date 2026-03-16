package com.monat.ecommerce.product.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
public record ProductResponse(
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
    
    // Specifications
    String weight,
    String dimensions,
    String color,
    String material,
    Map<String, String> additionalSpecs
) {}
