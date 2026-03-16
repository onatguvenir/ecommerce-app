package com.monat.ecommerce.product.domain.event;

import com.monat.ecommerce.product.domain.model.Product;
import org.springframework.context.ApplicationEvent;

/**
 * Domain Event: Ürün güncellendiğinde yayınlanır.
 * Elasticsearch read modelini asenkron günceller.
 */
public class ProductUpdatedEvent extends ApplicationEvent {

    private final Product product;

    public ProductUpdatedEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
