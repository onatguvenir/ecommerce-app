package com.monat.ecommerce.product.domain.service;

import com.monat.ecommerce.product.domain.event.ProductCreatedEvent;
import com.monat.ecommerce.product.domain.event.ProductDeletedEvent;
import com.monat.ecommerce.product.domain.event.ProductUpdatedEvent;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchDocument;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    private static final int REINDEX_BATCH_SIZE = 500;

    private final ProductSearchRepository searchRepository;
    private final ProductRepository productRepository;

    /**
     * Listener for ProductCreatedEvent.
     * 
     * @Async: This method runs independently of the event publisher's thread,
     *         in a separate thread (AsyncTaskExecutor).
     */
    @Async
    @EventListener
    public void onProductCreated(ProductCreatedEvent event) {
        log.debug("Async ES indexing triggered for new product: {}", event.getProduct().getProductId());
        indexProduct(event.getProduct());
    }

    /**
     * Listener for ProductUpdatedEvent.
     * Updates the existing document in ES (upsert semantics).
     */
    @Async
    @EventListener
    public void onProductUpdated(ProductUpdatedEvent event) {
        log.debug("Async ES re-indexing triggered for updated product: {}", event.getProduct().getProductId());
        indexProduct(event.getProduct());
    }

    /**
     * Listener for ProductDeletedEvent.
     * Removes the document from ES.
     */
    @Async
    @EventListener
    public void onProductDeleted(ProductDeletedEvent event) {
        log.debug("Async ES removal triggered for deleted product: {}", event.getProductId());
        removeFromIndex(event.getMongoId());
    }

    /**
     * Indexes only products that are present in MongoDB but missing from Elasticsearch.
     * Used on startup when ES has fewer documents than MongoDB (e.g., ES data loss,
     * products created while the app was down).
     * Per batch: one MongoDB read + one ES _mget — does not load all documents into memory.
     */
    public void indexMissingProducts() {
        log.info("Scanning for unindexed products...");
        int pageNum = 0;
        long totalIndexed = 0;

        Page<Product> page;
        do {
            page = productRepository.findAll(PageRequest.of(pageNum, REINDEX_BATCH_SIZE));
            if (page.isEmpty()) break;

            List<String> ids = page.getContent().stream()
                    .map(Product::getId)
                    .toList();

            Set<String> indexedIds = StreamSupport
                    .stream(searchRepository.findAllById(ids).spliterator(), false)
                    .map(ProductSearchDocument::getId)
                    .collect(Collectors.toSet());

            List<ProductSearchDocument> missing = page.getContent().stream()
                    .filter(p -> !indexedIds.contains(p.getId()))
                    .map(this::mapToSearchDocument)
                    .toList();

            if (!missing.isEmpty()) {
                searchRepository.saveAll(missing);
                totalIndexed += missing.size();
                log.info("Indexed {} missing products (page {})", missing.size(), pageNum);
            }

            pageNum++;
        } while (page.hasNext());

        log.info("Missing product index complete: {} products indexed", totalIndexed);
    }

    /**
     * Re-indexes all products using paginated MongoDB reads and ES bulk writes.
     * Avoids loading all products into memory and uses ES Bulk API for efficiency.
     */
    public void reindexAll() {
        log.info("Starting full ES reindex (batch size: {})...", REINDEX_BATCH_SIZE);
        int pageNum = 0;
        long totalIndexed = 0;

        Page<Product> page;
        do {
            page = productRepository.findAll(PageRequest.of(pageNum, REINDEX_BATCH_SIZE));
            if (page.isEmpty()) break;

            List<ProductSearchDocument> docs = page.getContent().stream()
                    .map(this::mapToSearchDocument)
                    .toList();

            searchRepository.saveAll(docs);
            totalIndexed += docs.size();
            pageNum++;

            log.info("Reindexed {}/{} products", totalIndexed, page.getTotalElements());
        } while (page.hasNext());

        log.info("Full ES reindex complete: {} products", totalIndexed);
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
