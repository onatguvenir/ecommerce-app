package com.monat.ecommerce.order.domain.event;

import java.util.UUID;

public record OrderSagaStartedEvent(UUID orderId, String cartId) {}
