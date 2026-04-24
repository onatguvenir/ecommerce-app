package com.monat.ecommerce.cart.domain.exception;

public class InsufficientStockException extends RuntimeException {

    private final String productId;
    private final int availableQuantity;

    public InsufficientStockException(String productId, int availableQuantity) {
        super("Insufficient stock for product " + productId + ": available=" + availableQuantity);
        this.productId = productId;
        this.availableQuantity = availableQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
