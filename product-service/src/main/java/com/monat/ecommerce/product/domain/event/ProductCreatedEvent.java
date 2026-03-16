package com.monat.ecommerce.product.domain.event;

import com.monat.ecommerce.product.domain.model.Product;
import org.springframework.context.ApplicationEvent;

/**
 * Domain Event: Yeni ürün oluşturulduğunda yayınlanır.
 * <p>
 * Domain Event Pattern: Domain'deki önemli olayları temsil eder.
 * ApplicationEvent extends etmek Spring'in event altyapısını kullanmamızı
 * sağlar.
 * <p>
 * Bu event'i dinleyen ProductSyncService, Elasticsearch'e asenkron olarak
 * index işlemi yapar. Bu sayede write path (MongoDB kaydetme) ile
 * read model güncelleme (ES indexleme) birbirinden ayrılır.
 * </p>
 */
public class ProductCreatedEvent extends ApplicationEvent {

    private final Product product;

    public ProductCreatedEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
