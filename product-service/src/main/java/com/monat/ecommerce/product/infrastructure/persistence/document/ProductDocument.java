package com.monat.ecommerce.product.infrastructure.persistence.document;

import com.monat.ecommerce.product.domain.model.ProductSpecifications;
import com.monat.ecommerce.product.domain.model.ProductStatus;
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
 * Product Document.
 * <p>
 * This class represents a mapping to the "products" collection in MongoDB.
 * Unlike JPA @Entity, MongoDB uses @Document.
 * </p>
 * 
 * @Document(collection = "products") tells Spring Data MongoDB that this class
 *                      is a Document and should be mapped to the "products"
 *                      collection.
 * 
 *                      @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
 *                      are Lombok annotations for boilerplate code reduction.
 */
@Document(collection = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String productId;

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
}
