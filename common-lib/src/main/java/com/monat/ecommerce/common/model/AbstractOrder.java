package com.monat.ecommerce.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base domain model for Orders and Carts.
 * Technology-agnostic, pure domain logic.
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractOrder<I> {
    protected String userId;
    protected BigDecimal totalAmount;
    protected String currency;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    @Builder.Default
    protected List<I> items = new ArrayList<>();

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<I> getItems() {
        return items;
    }

    public void setItems(List<I> items) {
        this.items = items;
    }

    public void calculateTotalAmount() {
        if (items == null || items.isEmpty()) {
            this.totalAmount = BigDecimal.ZERO;
            return;
        }
        this.totalAmount = items.stream()
                .map(item -> {
                    AbstractOrderItem i = (AbstractOrderItem) item;
                    if (i.getSubtotal() == null) {
                        i.calculateSubtotal();
                    }
                    return i.getSubtotal() != null ? i.getSubtotal() : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @JsonIgnore
    public Integer getTotalItems() {
        if (items == null) return 0;
        return items.stream()
                .mapToInt(item -> ((AbstractOrderItem) item).getQuantity())
                .sum();
    }
}
