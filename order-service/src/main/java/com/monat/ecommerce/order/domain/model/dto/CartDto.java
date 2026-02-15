package com.monat.ecommerce.order.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {
    private String cartId;
    private String userId;
    private List<CartItemDto> items;
    private BigDecimal totalAmount;
    private Integer totalItems;
}
