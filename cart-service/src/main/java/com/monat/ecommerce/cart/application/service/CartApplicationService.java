package com.monat.ecommerce.cart.application.service;

import com.monat.ecommerce.cart.application.dto.AddToCartRequest;
import com.monat.ecommerce.cart.application.dto.CartResponse;
import com.monat.ecommerce.cart.application.mapper.CartMapper;
import com.monat.ecommerce.cart.domain.model.Cart;
import com.monat.ecommerce.cart.domain.model.CartItem;
import com.monat.ecommerce.cart.domain.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;

/**
 * Coordinator for Shopping Cart operations.
 * 
 * Educational Note:
 * This service uses Redis to store active cart data. 
 * High-speed caching is essential here as users interact with their carts 
 * frequently during a single session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartApplicationService {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;

    @Value("${application.cart.max-items:100}")
    private Integer maxItems;

    /**
     * Get cart by cart ID (user ID or session ID)
     */
    public CartResponse getCart(String cartId) {
        log.debug("Getting cart: {}", cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseGet(() -> createNewCart(cartId));

        return cartMapper.toResponse(cart);
    }

    /**
     * Add item to cart
     */
    public CartResponse addToCart(String cartId, AddToCartRequest request) {
        log.info("Adding item to cart: {} - Product: {}", cartId, request.productId());

        Cart cart = cartRepository.findById(cartId)
                .orElseGet(() -> createNewCart(cartId));

        // Check max items limit
        if (cart.getTotalItems() >= maxItems) {
            throw new IllegalStateException("Cart has reached maximum capacity of " + maxItems + " items");
        }

        // Create cart item using MapStruct
        CartItem item = cartMapper.toItem(request);
        
        item.calculateSubtotal();

        // Add to cart
        cart.addItem(item);

        // Save cart
        cartRepository.save(cart);

        log.info("Item added to cart: {} - Total items: {}", cartId, cart.getTotalItems());

        return cartMapper.toResponse(cart);
    }

    /**
     * Update item quantity
     */
    public CartResponse updateItemQuantity(String cartId, String productId, Integer quantity) {
        log.info("Updating item quantity - Cart: {}, Product: {}, Qty: {}", cartId, productId, quantity);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        if (quantity <= 0) {
            cart.removeItem(productId);
        } else {
            cart.updateItemQuantity(productId, quantity);
        }

        cartRepository.save(cart);

        return cartMapper.toResponse(cart);
    }

    /**
     * Remove item from cart
     */
    public CartResponse removeItem(String cartId, String productId) {
        log.info("Removing item - Cart: {}, Product: {}", cartId, productId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        cart.removeItem(productId);
        cartRepository.save(cart);

        return cartMapper.toResponse(cart);
    }

    /**
     * Clear cart
     */
    public void clearCart(String cartId) {
        log.info("Clearing cart: {}", cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        cart.clear();
        cartRepository.save(cart);
    }

    /**
     * Delete cart
     */
    public void deleteCart(String cartId) {
        log.info("Deleting cart: {}", cartId);
        cartRepository.delete(cartId);
    }

    /**
     * Merge anonymous cart with user cart on login
     */
    public CartResponse mergeCart(String anonymousCartId, String userId) {
        log.info("Merging carts - Anonymous: {}, User: {}", anonymousCartId, userId);

        Cart anonymousCart = cartRepository.findById(anonymousCartId).orElse(null);
        Cart userCart = cartRepository.findById(userId)
                .orElseGet(() -> createNewCart(userId));

        if (anonymousCart != null && !anonymousCart.isEmpty()) {
            userCart.merge(anonymousCart);
            cartRepository.save(userCart);
            cartRepository.delete(anonymousCartId);
            log.info("Carts merged successfully - Total items: {}", userCart.getTotalItems());
        }

        return cartMapper.toResponse(userCart);
    }

    private Cart createNewCart(String cartId) {
        return Cart.builder()
                .cartId(cartId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
