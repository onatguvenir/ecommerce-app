package com.monat.ecommerce.order.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shared JPA fields for Order Item related entities.
 */
@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractOrderItemEntity {

    @Id
    protected UUID id;

    @Column(name = "product_id", nullable = false)
    protected String productId;

    @Column(name = "product_name", nullable = false)
    protected String productName;

    @Column(nullable = false)
    protected Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    protected BigDecimal unitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    protected BigDecimal subtotal;
}
