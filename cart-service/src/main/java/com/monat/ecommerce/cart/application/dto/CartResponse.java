package com.monat.ecommerce.cart.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CartResponse(
    String cartId,
    String userId,
    List<CartItemResponse> items,
    BigDecimal totalAmount,
    Integer totalItems
) {}
