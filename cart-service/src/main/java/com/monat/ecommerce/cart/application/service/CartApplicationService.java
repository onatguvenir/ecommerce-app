package com.monat.ecommerce.cart.application.service;

import com.monat.ecommerce.cart.application.dto.AddToCartRequest;
import com.monat.ecommerce.cart.application.dto.CartResponse;
import com.monat.ecommerce.cart.application.mapper.CartMapper;
import com.monat.ecommerce.cart.domain.exception.InsufficientStockException;
import com.monat.ecommerce.cart.domain.model.Cart;
import com.monat.ecommerce.cart.domain.model.CartItem;
import com.monat.ecommerce.cart.domain.repository.CartRepository;
import com.monat.ecommerce.cart.infrastructure.config.CartLockService;
import com.monat.ecommerce.cart.infrastructure.config.CartMetrics;
import com.monat.ecommerce.grpc.inventory.CheckStockRequest;
import com.monat.ecommerce.grpc.inventory.CheckStockResponse;
import com.monat.ecommerce.grpc.inventory.InventoryServiceGrpc;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Coordinator for Shopping Cart operations.
 *
 * <p>Concurrency Strategy — Distributed Redis Lock:
 * Every mutating operation (add, update, remove, clear, merge) follows the
 * Read-Modify-Write pattern against Redis. Without synchronisation, two concurrent
 * requests for the same cart can both read stale state and silently overwrite each other
 * (lost update).
 *
 * <p>To prevent this, each write operation acquires a per-cart distributed lock via
 * {@link CartLockService} before reading the cart. The lock is backed by Redisson
 * (Redis) and is automatically released after the operation completes (or on failure).
 * Callers receive a {@link CartLockService.CartLockException} if the lock cannot be
 * acquired within the configured timeout, which should be mapped to HTTP 429/503.
 *
 * <p>Read operations (getCart) are intentionally left unlocked — reading a slightly
 * stale snapshot is acceptable and keeps read latency minimal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartApplicationService {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final CartMetrics cartMetrics;
    private final CartLockService cartLockService;

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

    @Value("${application.cart.max-items:100}")
    private Integer maxItems;

    /**
     * Get cart by cart ID — read is lock-free for performance.
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
     * Add item to cart.
     *
     * <p>Lock scope: The lock is held for the entire read-modify-write cycle to prevent
     * two simultaneous add-item requests from both reading the same cart state and then
     * overwriting each other's updates (classic lost update).
     */
    public CartResponse addToCart(String cartId, AddToCartRequest request) {
        checkStockAvailability(request.productId(), request.quantity());
        Timer.Sample sample = Timer.start();
        log.info("Adding item to cart: {} - Product: {}", cartId, request.productId());
        try {
            return cartLockService.executeWithLock(cartId, () -> {
                Cart cart = cartRepository.findById(cartId)
                        .orElseGet(() -> createNewCart(cartId));

                if (cart.getTotalItems() >= maxItems) {
                    cartMetrics.incrementOperation("add", "capacity_reached");
                    throw new IllegalStateException(
                            "Cart has reached maximum capacity of " + maxItems + " items");
                }

                CartItem item = cartMapper.toItem(request);
                item.calculateSubtotal();
                cart.addItem(item);
                cartRepository.save(cart);

                log.info("Item added to cart: {} - Total items: {}", cartId, cart.getTotalItems());
                cartMetrics.incrementOperation("add", "success");
                cartMetrics.recordCartSize(cart.getTotalItems(), "add");
                return cartMapper.toResponse(cart);
            });
        } catch (CartLockService.CartLockException ex) {
            cartMetrics.incrementOperation("add", "lock_timeout");
            throw ex;
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("add", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    /**
     * Update item quantity.
     *
     * <p>Lock scope: Prevents concurrent quantity updates from interleaving and producing
     * an incorrect final quantity.
     */
    public CartResponse updateItemQuantity(String cartId, String productId, Integer quantity) {
        Timer.Sample sample = Timer.start();
        log.info("Updating item quantity - Cart: {}, Product: {}, Qty: {}", cartId, productId, quantity);
        try {
            return cartLockService.executeWithLock(cartId, () -> {
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
            });
        } catch (CartLockService.CartLockException ex) {
            cartMetrics.incrementOperation("update_quantity", "lock_timeout");
            throw ex;
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("update_quantity", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    /**
     * Remove item from cart.
     *
     * <p>Lock scope: Guards against race conditions where item removal and quantity update
     * overlap on the same cart.
     */
    public CartResponse removeItem(String cartId, String productId) {
        Timer.Sample sample = Timer.start();
        log.info("Removing item - Cart: {}, Product: {}", cartId, productId);
        try {
            return cartLockService.executeWithLock(cartId, () -> {
                Cart cart = cartRepository.findById(cartId)
                        .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

                cart.removeItem(productId);
                cartRepository.save(cart);
                cartMetrics.incrementOperation("remove", "success");
                cartMetrics.recordCartSize(cart.getTotalItems(), "remove");
                return cartMapper.toResponse(cart);
            });
        } catch (CartLockService.CartLockException ex) {
            cartMetrics.incrementOperation("remove", "lock_timeout");
            throw ex;
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("remove", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    /**
     * Clear all items from the cart.
     */
    public void clearCart(String cartId) {
        Timer.Sample sample = Timer.start();
        log.info("Clearing cart: {}", cartId);
        try {
            cartLockService.executeWithLock(cartId, () -> {
                Cart cart = cartRepository.findById(cartId)
                        .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

                cart.clear();
                cartRepository.save(cart);
                cartMetrics.incrementOperation("clear", "success");
                cartMetrics.recordCartSize(cart.getTotalItems(), "clear");
            });
        } catch (CartLockService.CartLockException ex) {
            cartMetrics.incrementOperation("clear", "lock_timeout");
            throw ex;
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("clear", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    /**
     * Delete the cart entirely — no lock needed; delete is idempotent.
     */
    @Observed(name = "cart.operation", contextualName = "cart-delete")
    public void deleteCart(String cartId) {
        log.info("Deleting cart: {}", cartId);
        cartRepository.delete(cartId);
        cartMetrics.incrementOperation("delete", "success");
    }

    /**
     * Merge anonymous cart with user cart on login.
     *
     * <p>Lock scope: Locks the user cart (destination) during the merge to prevent
     * concurrent add-item requests from overwriting merged items. The anonymous cart
     * is only read, so it does not need a lock.
     */
    public CartResponse mergeCart(String anonymousCartId, String userId) {
        Timer.Sample sample = Timer.start();
        log.info("Merging carts - Anonymous: {}, User: {}", anonymousCartId, userId);
        try {
            return cartLockService.executeWithLock(userId, () -> {
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
            });
        } catch (CartLockService.CartLockException ex) {
            cartMetrics.incrementOperation("merge", "lock_timeout");
            throw ex;
        } catch (RuntimeException ex) {
            cartMetrics.incrementOperation("merge", "failure");
            throw ex;
        } finally {
            sample.stop(cartMetrics.cartOperationTimer());
        }
    }

    private void checkStockAvailability(String productId, int requestedQuantity) {
        try {
            CheckStockResponse stock = inventoryStub.checkStock(
                    CheckStockRequest.newBuilder().setProductId(productId).build());
            if (stock.getAvailableQuantity() < requestedQuantity) {
                throw new InsufficientStockException(productId, stock.getAvailableQuantity());
            }
        } catch (InsufficientStockException ex) {
            throw ex;
        } catch (StatusRuntimeException ex) {
            // Fail-open: inventory service unavailable, stock enforced at order time
            log.warn("Stock check unavailable for product {}, proceeding: {}", productId, ex.getStatus().getCode());
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
