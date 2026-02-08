package com.monat.ecommerce.product.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product document stored in MongoDB
 */
@Document(collection = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private String id;

    @Indexed(unique = true)
    private String productId;  // Business ID (e.g., PROD-001)

    @Indexed
    private String name;

    private String description;

    @Indexed
    private String category;

    private String brand;

    private BigDecimal price;

    private String currency;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private ProductSpecifications specifications;

    @Indexed
    private ProductStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
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
