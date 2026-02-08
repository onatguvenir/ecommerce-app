package com.monat.ecommerce.cart.domain.repository;

import com.monat.ecommerce.cart.domain.model.Cart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Redis-based cart repository
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CartRepository {

    private static final String CART_KEY_PREFIX = "cart:";

    private final RedisTemplate<String, Cart> redisTemplate;

    @Value("${application.cart.expiry-days:7}")
    private Integer expiryDays;

    /**
     * Save cart with TTL
     */
    public Cart save(Cart cart) {
        String key = buildKey(cart.getCartId());
        redisTemplate.opsForValue().set(key, cart, Duration.ofDays(expiryDays));
        log.debug("Cart saved: {}, expires in {} days", cart.getCartId(), expiryDays);
        return cart;
    }

    /**
     * Find cart by ID
     */
    public Optional<Cart> findById(String cartId) {
        String key = buildKey(cartId);
        Cart cart = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(cart);
    }

    /**
     * Delete cart
     */
    public void delete(String cartId) {
        String key = buildKey(cartId);
        redisTemplate.delete(key);
        log.debug("Cart deleted: {}", cartId);
    }

    /**
     * Check if cart exists
     */
    public boolean exists(String cartId) {
        String key = buildKey(cartId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Extend cart TTL
     */
    public void extendTTL(String cartId) {
        String key = buildKey(cartId);
        redisTemplate.expire(key, Duration.ofDays(expiryDays));
        log.debug("Cart TTL extended: {}", cartId);
    }

    /**
     * Delete all carts (Warning: uses KEYS command, careful in production)
     */
    public void deleteAll() {
        Set<String> keys = redisTemplate.keys(CART_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.debug("All carts deleted");
    }

    private String buildKey(String cartId) {
        return CART_KEY_PREFIX + cartId;
    }
}
