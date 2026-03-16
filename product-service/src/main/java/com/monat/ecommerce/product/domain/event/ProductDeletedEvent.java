package com.monat.ecommerce.product.domain.event;

import org.springframework.context.ApplicationEvent;

/**
 * Domain Event: Ürün silindiğinde yayınlanır.
 * Elasticsearch'ten asenkron kaldırır.
 */
public class ProductDeletedEvent extends ApplicationEvent {

    private final String mongoId;
    private final String productId;

    public ProductDeletedEvent(Object source, String mongoId, String productId) {
        super(source);
        this.mongoId = mongoId;
        this.productId = productId;
    }

    public String getMongoId() {
        return mongoId;
    }

    public String getProductId() {
        return productId;
    }
}
