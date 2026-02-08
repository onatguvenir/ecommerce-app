package com.monat.ecommerce.cart.infrastructure.controller;

import com.monat.ecommerce.cart.application.dto.AddToCartRequest;
import com.monat.ecommerce.cart.application.dto.CartResponse;
import com.monat.ecommerce.cart.application.service.CartApplicationService;
import com.monat.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart", description = "Shopping cart operations")
public class CartController {

    private final CartApplicationService cartService;

    @GetMapping("/{cartId}")
    @Operation(summary = "Get cart by ID", description = "Get cart for user or anonymous session")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@PathVariable String cartId) {
        CartResponse cart = cartService.getCart(cartId);

        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cart)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{cartId}/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @PathVariable String cartId,
            @Valid @RequestBody AddToCartRequest request) {

        CartResponse cart = cartService.addToCart(cartId, request);

        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cart)
                .message("Item added to cart")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Update item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable String cartId,
            @PathVariable String productId,
            @RequestParam Integer quantity) {

        CartResponse cart = cartService.updateItemQuantity(cartId, productId, quantity);

        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cart)
                .message("Item quantity updated")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable String cartId,
            @PathVariable String productId) {

        CartResponse cart = cartService.removeItem(cartId, productId);

        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cart)
                .message("Item removed from cart")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{cartId}")
    @Operation(summary = "Clear cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable String cartId) {
        cartService.clearCart(cartId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Cart cleared")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge anonymous cart with user cart", 
               description = "Merge anonymous session cart into authenticated user cart on login")
    public ResponseEntity<ApiResponse<CartResponse>> mergeCart(
            @RequestParam String anonymousCartId,
            @RequestParam String userId) {

        CartResponse cart = cartService.mergeCart(anonymousCartId, userId);

        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cart)
                .message("Carts merged successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
