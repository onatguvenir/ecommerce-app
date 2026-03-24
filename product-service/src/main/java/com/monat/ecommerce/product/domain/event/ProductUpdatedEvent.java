package com.monat.ecommerce.product.domain.event;

import com.monat.ecommerce.product.domain.model.Product;
import org.springframework.context.ApplicationEvent;

/**
 * Domain Event: Published when a product is updated.
 * Triggers an asynchronous update of the Elasticsearch read model.
 */
public class ProductUpdatedEvent extends ApplicationEvent {

    private final transient Product product;

    public ProductUpdatedEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
