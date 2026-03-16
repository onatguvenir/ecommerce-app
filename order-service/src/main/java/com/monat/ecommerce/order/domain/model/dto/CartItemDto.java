package com.monat.ecommerce.order.domain.model.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CartItemDto(
    String productId,
    String productName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subTotal
) {}
