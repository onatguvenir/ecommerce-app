package com.monat.ecommerce.order.domain.model.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CartDto(
    String cartId,
    String userId,
    List<CartItemDto> items,
    BigDecimal totalAmount,
    Integer totalItems
) {}
