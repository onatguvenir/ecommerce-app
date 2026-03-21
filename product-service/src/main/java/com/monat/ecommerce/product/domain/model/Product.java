package com.monat.ecommerce.product.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product Domain Model.
 * 
 * Educational Note:
 * In this system, 'id' is the persistence identifier (MongoDB ObjectId string), 
 * while 'productId' is the business identifier (e.g., SKU or human-readable ID).
 * This separation allows us to change database providers without affecting 
 * business logic that relies on 'productId'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private String id;
    private String productId; // Business ID (e.g., PROD-001)
    private String name;
    private String description;
    private String category;
    private String brand;
    private BigDecimal price;
    private String currency;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private ProductSpecifications specifications;
    private ProductStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private Long version = 0L;

    // Helper methods
    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
    }

    public boolean isActive() {
        return this.status == ProductStatus.ACTIVE;
    }
}
