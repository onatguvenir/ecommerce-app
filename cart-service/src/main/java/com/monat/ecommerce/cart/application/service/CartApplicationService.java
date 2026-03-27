package com.monat.ecommerce.cart.application.service;

import com.monat.ecommerce.cart.application.dto.AddToCartRequest;
import com.monat.ecommerce.cart.application.dto.CartResponse;
import com.monat.ecommerce.cart.application.mapper.CartMapper;
import com.monat.ecommerce.cart.domain.model.Cart;
import com.monat.ecommerce.cart.domain.model.CartItem;
import com.monat.ecommerce.cart.domain.repository.CartRepository;
import com.monat.ecommerce.cart.infrastructure.config.CartMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
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
    private final CartMetrics cartMetrics;

    @Value("${application.cart.max-items:100}")
    private Integer maxItems;

    /**
     * Get cart by cart ID (user ID or session ID)
     */
    @Observed(name = "cart.operation", contextualName = "cart-get")
    public CartResponse getCart(String cartId) {
        log.debug("Getting cart: {}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseGet(() -> createNewCart(cartId));
        cartMetrics.incrementOperation("get", "success");
        return cartMapper.toResponse(cart);
    }

    /**
     * Add item to cart
     */
    public CartResponse addToCart(String cartId, AddToCartRequest request) {
        Timer.Sample sample = Timer.start();
        log.info("Adding item to cart: {} - Product: {}", cartId, request.productId());
        try {
            Cart cart = cartRepository.findById(cartId)
                    .orElseGet(() -> createNewCart(cartId));

            if (cart.getTotalItems() >= maxItems) {
                cartMetrics.incrementOperation("add", "capacity_reached");
                throw new IllegalStateException("Cart has reached maximum capacity of " + maxItems + " items");
            }

            CartItem item = cartMapper.toItem(request);
            item.calculateSubtotal();
            cart.addItem(item);
            cartRepository.save(cart);

            log.info("Item added to cart: {} - Total items: {}", cartId, cart.getTotalItems());
            cartMetrics.incrementOperation("add", "success");
            cartMetrics.recordCartSize(cart.getTotalItems(), "add");
            return cartMapper.toResponse(cart);
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("add", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    /**
     * Update item quantity
     */
    public CartResponse updateItemQuantity(String cartId, String productId, Integer quantity) {
        Timer.Sample sample = Timer.start();
        log.info("Updating item quantity - Cart: {}, Product: {}, Qty: {}", cartId, productId, quantity);
        try {
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

            if (quantity <= 0) {
                cart.removeItem(productId);
            } else {
                cart.updateItemQuantity(productId, quantity);
            }

            cartRepository.save(cart);
            cartMetrics.incrementOperation("update_quantity", "success");
            cartMetrics.recordCartSize(cart.getTotalItems(), "update_quantity");
            return cartMapper.toResponse(cart);
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("update_quantity", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    /**
     * Remove item from cart
     */
    public CartResponse removeItem(String cartId, String productId) {
        Timer.Sample sample = Timer.start();
        log.info("Removing item - Cart: {}, Product: {}", cartId, productId);
        try {
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

            cart.removeItem(productId);
            cartRepository.save(cart);
            cartMetrics.incrementOperation("remove", "success");
            cartMetrics.recordCartSize(cart.getTotalItems(), "remove");
            return cartMapper.toResponse(cart);
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("remove", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    /**
     * Clear cart
     */
    public void clearCart(String cartId) {
        Timer.Sample sample = Timer.start();
        log.info("Clearing cart: {}", cartId);
        try {
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

            cart.clear();
            cartRepository.save(cart);
            cartMetrics.incrementOperation("clear", "success");
            cartMetrics.recordCartSize(cart.getTotalItems(), "clear");
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("clear", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    /**
     * Delete cart
     */
    @Observed(name = "cart.operation", contextualName = "cart-delete")
    public void deleteCart(String cartId) {
        log.info("Deleting cart: {}", cartId);
        cartRepository.delete(cartId);
        cartMetrics.incrementOperation("delete", "success");
    }

    /**
     * Merge anonymous cart with user cart on login
     */
    public CartResponse mergeCart(String anonymousCartId, String userId) {
        Timer.Sample sample = Timer.start();
        log.info("Merging carts - Anonymous: {}, User: {}", anonymousCartId, userId);
        try {
            Cart anonymousCart = cartRepository.findById(anonymousCartId).orElse(null);
            Cart userCart = cartRepository.findById(userId)
                    .orElseGet(() -> createNewCart(userId));

            if (anonymousCart != null && !anonymousCart.isEmpty()) {
                userCart.merge(anonymousCart);
                cartRepository.save(userCart);
                cartRepository.delete(anonymousCartId);
                log.info("Carts merged successfully - Total items: {}", userCart.getTotalItems());
            }

            cartMetrics.incrementOperation("merge", "success");
            cartMetrics.recordCartSize(userCart.getTotalItems(), "merge");
            return cartMapper.toResponse(userCart);
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("merge", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    private Cart createNewCart(String cartId) {
        return Cart.builder()
                .cartId(cartId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
