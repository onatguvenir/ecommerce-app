package com.monat.ecommerce.product.domain.event;

import com.monat.ecommerce.product.domain.model.Product;
import org.springframework.context.ApplicationEvent;

/**
 * Domain Event: Published when a new product is created.
 * <p>
 * Domain Event Pattern: Represents significant occurrences within the domain.
 * Extending ApplicationEvent integrates the event with Spring's event infrastructure.
 * <p>
 * This event is consumed by ProductSyncService to trigger asynchronous indexing 
 * in Elasticsearch. This separates the write path (MongoDB persistence) from 
 * the read model update (ES indexing).
 * </p>
 */
public class ProductCreatedEvent extends ApplicationEvent {

    private final transient Product product;

    public ProductCreatedEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
