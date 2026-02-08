package com.monat.ecommerce.product.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;
    private String productId;
    private String name;
    private String description;
    private String category;
    private String brand;
    private BigDecimal price;
    private String currency;
    private List<String> images;
    private List<String> tags;
    private String status;
    
    // Specifications
    private String weight;
    private String dimensions;
    private String color;
    private String material;
    private Map<String, String> additionalSpecs;
}
