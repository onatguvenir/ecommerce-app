package com.monat.ecommerce.cart.domain.model;

import com.monat.ecommerce.common.model.AbstractOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Shopping cart model stored in Redis.
 * Extends AbstractOrder to share common structure with Order.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Cart extends com.monat.ecommerce.common.model.AbstractOrder<CartItem> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cartId;  // User ID or session ID

    /**
     * Add item to cart or update quantity if already exists
     */
    public void addItem(CartItem item) {
        CartItem existingItem = findItem(item.getProductId());
        
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            existingItem.calculateSubtotal();
        } else {
            if (this.items == null) this.items = new ArrayList<>();
            item.calculateSubtotal();
            this.items.add(item);
        }
        
        this.updatedAt = LocalDateTime.now();
        this.calculateTotalAmount();
    }

    public CartItem findItem(String productId) {
        if (items == null) return null;
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    public void removeItem(String productId) {
        if (items != null) {
            items.removeIf(item -> item.getProductId().equals(productId));
            this.calculateTotalAmount();
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void updateItemQuantity(String productId, Integer quantity) {
        CartItem item = findItem(productId);
        if (item != null) {
            if (quantity <= 0) {
                removeItem(productId);
            } else {
                item.setQuantity(quantity);
                item.calculateSubtotal();
                this.calculateTotalAmount();
                this.updatedAt = LocalDateTime.now();
            }
        }
    }

    public void clear() {
        if (items != null) {
            items.clear();
            this.calculateTotalAmount();
            this.updatedAt = LocalDateTime.now();
        }
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public void merge(Cart other) {
        if (other != null && other.getItems() != null) {
            other.getItems().forEach(this::addItem);
            this.updatedAt = LocalDateTime.now();
        }
    }
}
