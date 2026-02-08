package com.monat.ecommerce.product.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Product specifications/attributes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecifications {
    
    private String weight;
    private String dimensions;
    private String color;
    private String material;
    
    @Builder.Default
    private Map<String, String> additionalSpecs = new HashMap<>();

    public void addSpec(String key, String value) {
        additionalSpecs.put(key, value);
    }
}
