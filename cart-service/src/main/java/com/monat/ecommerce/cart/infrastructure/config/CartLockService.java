package com.monat.ecommerce.cart.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed Lock service for Cart operations using Redisson.
 *
 * <p>Problem solved: The cart service performs Read-Modify-Write operations against Redis.
 * Without a distributed lock, two concurrent requests for the same cart (e.g., user opening
 * two browser tabs and rapidly adding items) can both read the same stale state, apply
 * their changes independently, and then overwrite each other — resulting in a lost update.
 *
 * <p>Solution: For each write operation, we acquire a per-cart lock in Redis before reading
 * the cart state. This ensures mutual exclusion at the application level across all
 * cart-service instances, while the lock itself lives in Redis (already part of our infra).
 *
 * <p>Lock key pattern: {@code cart-lock:{cartId}}
 *
 * <p>Design decisions:
 * <ul>
 *   <li>waitTime — maximum time a request will wait to acquire the lock before failing.
 *       Keeps latency bounded; better to fail fast than hang indefinitely.</li>
 *   <li>leaseTime — maximum time a lock is held. Prevents deadlock if the process crashes
 *       while holding the lock (Redisson watchdog extends this automatically if needed).</li>
 *   <li>Fairness — uses a regular (non-fair) lock for performance. Fair locks add overhead
 *       and are unnecessary here since lost-update prevention is the only goal.</li>
 * </ul>
 */
@Slf4j
@Component
public class CartLockService {

    private static final String LOCK_KEY_PREFIX = "cart-lock:";

    private final RedissonClient redissonClient;

    @Value("${application.cart.lock.wait-time-ms:3000}")
    private long waitTimeMs;

    @Value("${application.cart.lock.lease-time-ms:10000}")
    private long leaseTimeMs;

    public CartLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Executes {@code action} while holding the distributed lock for the given cart.
     *
     * <p>If the lock cannot be acquired within {@code waitTimeMs}, a
     * {@link CartLockException} is thrown so the caller can return an appropriate
     * HTTP 429 / 503 response instead of silently corrupting the cart state.
     *
     * @param cartId the cart identifier to lock
     * @param action the business logic to execute inside the critical section
     * @param <T>    return type of the action
     * @return result of {@code action}
     * @throws CartLockException if the lock cannot be acquired in time
     */
    public <T> T executeWithLock(String cartId, Supplier<T> action) {
        String lockKey = LOCK_KEY_PREFIX + cartId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            // tryLock is non-blocking up to waitTimeMs, then fails fast
            acquired = lock.tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS);

            if (!acquired) {
                log.warn("Could not acquire cart lock within {}ms for cartId: {}", waitTimeMs, cartId);
                throw new CartLockException("Cart is currently being modified. Please retry. cartId=" + cartId);
            }

            log.debug("Cart lock acquired for cartId: {}", cartId);
            return action.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt status
            throw new CartLockException("Interrupted while waiting for cart lock. cartId=" + cartId, e);

        } finally {
            // Only unlock if this thread currently holds the lock.
            // isHeldByCurrentThread() guard prevents IllegalMonitorStateException
            // in edge cases (e.g., lease expired before unlock is called).
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Cart lock released for cartId: {}", cartId);
            }
        }
    }

    /**
     * Void variant — delegates to {@link #executeWithLock(String, Supplier)}.
     *
     * @param cartId the cart identifier to lock
     * @param action the void action to execute
     */
    public void executeWithLock(String cartId, Runnable action) {
        executeWithLock(cartId, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Unchecked exception thrown when a cart lock cannot be acquired.
     * Callers should map this to HTTP 429 Too Many Requests or 503 Service Unavailable.
     */
    public static class CartLockException extends RuntimeException {
        public CartLockException(String message) {
            super(message);
        }
        public CartLockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
