package com.monat.ecommerce.order.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Builder
public record CreateOrderRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @Nullable String cartId,

        @Valid @Nullable
        List<OrderItemRequest> items,

        @Valid @Nullable
        AddressRequest shippingAddress
) {
}
