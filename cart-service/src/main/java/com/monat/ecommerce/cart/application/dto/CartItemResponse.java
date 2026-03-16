package com.monat.ecommerce.cart.application.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CartItemResponse(
    String productId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal subtotal,
    String imageUrl
) {}
