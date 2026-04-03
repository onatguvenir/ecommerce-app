package com.monat.ecommerce.order.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CreateOrderRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        String cartId,

        @Valid
        List<OrderItemRequest> items,

        @Valid
        AddressRequest shippingAddress
) {
}
