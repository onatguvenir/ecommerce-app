package com.monat.ecommerce.cart.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Shopping cart model stored in Redis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cartId;  // User ID or session ID
    private String userId;  // Null for anonymous carts
    
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Add item to cart or update quantity if already exists
     */
    public void addItem(CartItem item) {
        CartItem existingItem = findItem(item.getProductId());
        
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            existingItem.setSubtotal(existingItem.getUnitPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
        } else {
            items.add(item);
        }
        
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update item quantity
     */
    public void updateItemQuantity(String productId, Integer quantity) {
        CartItem item = findItem(productId);
        if (item != null) {
            item.setQuantity(quantity);
            item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Remove item from cart
     */
    public void removeItem(String productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Clear all items
     */
    public void clear() {
        items.clear();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate total amount
     */
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total item count
     */
    public Integer getTotalItems() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Merge another cart into this one
     */
    public void merge(Cart otherCart) {
        if (otherCart == null || otherCart.getItems().isEmpty()) {
            return;
        }

        for (CartItem otherItem : otherCart.getItems()) {
            this.addItem(otherItem);
        }
    }

    private CartItem findItem(String productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
