package com.monat.ecommerce.order.infrastructure.client;

import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.order.domain.model.dto.CartDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service", url = "${application.config.cart-service-url:http://cart-service:8084}")
public interface CartClient {

    @GetMapping("/api/cart/{cartId}")
    ApiResponse<CartDto> getCart(@PathVariable("cartId") String cartId);

    // Idempotent removal of the whole cart. Targets the dedicated internal endpoint —
    // NOT DELETE /api/cart/{cartId}, which is the customer "clear cart" operation and
    // throws when the cart is absent.
    @DeleteMapping("/api/cart/internal/{cartId}")
    ApiResponse<Void> deleteCart(@PathVariable("cartId") String cartId);
}
