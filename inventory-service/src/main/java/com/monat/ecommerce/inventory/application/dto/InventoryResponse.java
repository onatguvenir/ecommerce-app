package com.monat.ecommerce.inventory.application.dto;

import lombok.Builder;

@Builder
public record InventoryResponse(
    String productId,
    Integer quantity,
    Integer availableQuantity,
    Integer reservedQuantity
) {}
