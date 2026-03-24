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
 * Elasticsearch Synchronization Service — Event-Driven & Async.
 * <p>
 * Previous implementation: ProductApplicationService directly called this service.
 * This made the write path (MongoDB save) dependent on ES latency.
 * <p>
 * New Approach — Domain Event Pattern:
 * 1. CommandHandler writes to MongoDB and publishes a domain event.
 * 2. This service catches the event via @EventListener.
 * 3. @Async ensures ES indexing happens in a separate thread pool.
 * <p>
 * Advantages:
 * - Lower Write Latency: MongoDB writes are unaffected if ES slows down.
 * - Loose Coupling: CommandHandlers are unaware of ES.
 * - Resilience: If ES is temporarily unavailable, event processing can be 
 *   retried (future enhancement).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductSearchRepository searchRepository;

    /**
     * Listener for ProductCreatedEvent.
     * 
     * @Async: This method runs independently of the event publisher's thread,
     *         in a separate thread (AsyncTaskExecutor).
     */
    @Async
    @EventListener
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Async ES indexing triggered for new product: {}", event.getProduct().getProductId());
        indexProduct(event.getProduct());
    }

    /**
     * Listener for ProductUpdatedEvent.
     * Updates the existing document in ES (upsert semantics).
     */
    @Async
    @EventListener
    public void onProductUpdated(ProductUpdatedEvent event) {
        log.info("Async ES re-indexing triggered for updated product: {}", event.getProduct().getProductId());
        indexProduct(event.getProduct());
    }

    /**
     * Listener for ProductDeletedEvent.
     * Removes the document from ES.
     */
    @Async
    @EventListener
    public void onProductDeleted(ProductDeletedEvent event) {
        log.info("Async ES removal triggered for deleted product: {}", event.getProductId());
        removeFromIndex(event.getMongoId());
    }

    /**
     * Re-indexes all products (maintenance operation).
     * Can be called from an admin endpoint or a scheduled job.
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
            // ES failure does not affect the write path — it is only logged.
            // Future Enhancement: Dead Letter Queue or retry mechanism can be added.
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
