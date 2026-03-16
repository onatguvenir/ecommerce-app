package com.monat.ecommerce.product.domain.service;

import com.monat.ecommerce.product.domain.event.ProductCreatedEvent;
import com.monat.ecommerce.product.domain.event.ProductDeletedEvent;
import com.monat.ecommerce.product.domain.event.ProductUpdatedEvent;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchDocument;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Elasticsearch Senkronizasyon Servisi — Event-Driven & Async.
 * <p>
 * Önceki implementasyonda ProductApplicationService doğrudan bu servisi
 * çağırıyordu.
 * Bu durum write path'i (MongoDB kaydetme) ES latency'sine bağımlı kılıyordu.
 * <p>
 * Yeni yaklaşım — Domain Event Pattern:
 * 1. CommandHandler, MongoDB'ye yazar ve domain event yayınlar.
 * 2. Bu servis @EventListener ile event'i yakalar.
 * 3. @Async sayesinde ayrı bir thread pool'da ES indexleme yapar.
 * <p>
 * Avantajlar:
 * - Write latency düşer: ES yavaşlasa bile MongoDB yazma etkilenmez.
 * - Loose coupling: CommandHandler, ES'ten haberdar değil.
 * - Resilience: ES geçici olarak erişilemez olsa bile event işleme
 * retry mekanizması ile tekrar denenebilir (future enhancement).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductSearchRepository searchRepository;

    /**
     * ProductCreatedEvent dinleyicisi.
     * 
     * @Async: Bu metod, event yayıncısının thread'inden bağımsız,
     *         ayrı bir thread'de (AsyncTaskExecutor) çalışır.
     */
    @Async
    @EventListener
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Async ES indexing triggered for new product: {}", event.getProduct().getProductId());
        indexProduct(event.getProduct());
    }

    /**
     * ProductUpdatedEvent dinleyicisi.
     * ES'teki mevcut document'ı günceller (upsert semantiği).
     */
    @Async
    @EventListener
    public void onProductUpdated(ProductUpdatedEvent event) {
        log.info("Async ES re-indexing triggered for updated product: {}", event.getProduct().getProductId());
        indexProduct(event.getProduct());
    }

    /**
     * ProductDeletedEvent dinleyicisi.
     * ES'ten document'ı kaldırır.
     */
    @Async
    @EventListener
    public void onProductDeleted(ProductDeletedEvent event) {
        log.info("Async ES removal triggered for deleted product: {}", event.getProductId());
        removeFromIndex(event.getMongoId());
    }

    /**
     * Tüm ürünleri yeniden index'ler (bakım operasyonu).
     * Admin endpoint'ten veya scheduled job'dan çağrılabilir.
     */
    public void reindexAll(Iterable<Product> products) {
        log.info("Starting full ES reindex...");
        int count = 0;
        for (Product product : products) {
            indexProduct(product);
            count++;
        }
        log.info("Reindexed {} products in Elasticsearch", count);
    }

    // ---- Private helpers ----

    private void indexProduct(Product product) {
        try {
            ProductSearchDocument searchDoc = mapToSearchDocument(product);
            searchRepository.save(searchDoc);
            log.debug("Product indexed in Elasticsearch: {}", product.getProductId());
        } catch (Exception e) {
            // ES hatası write path'i etkilemez — sadece loglanır.
            // Gelecekte: Dead Letter Queue veya retry mekanizması eklenebilir.
            log.error("Failed to index product in Elasticsearch: {}", product.getProductId(), e);
        }
    }

    private void removeFromIndex(String mongoId) {
        try {
            searchRepository.deleteById(mongoId);
            log.debug("Product removed from Elasticsearch index: {}", mongoId);
        } catch (Exception e) {
            log.error("Failed to remove product from Elasticsearch: {}", mongoId, e);
        }
    }

    private ProductSearchDocument mapToSearchDocument(Product product) {
        return ProductSearchDocument.builder()
                .id(product.getId())
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .tags(product.getTags())
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .build();
    }
}
