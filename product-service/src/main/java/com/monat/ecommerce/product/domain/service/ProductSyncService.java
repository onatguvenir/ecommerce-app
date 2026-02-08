package com.monat.ecommerce.product.domain.service;

import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchDocument;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to sync MongoDB products with Elasticsearch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductSearchRepository searchRepository;

    /**
     * Index product in Elasticsearch
     */
    public void indexProduct(Product product) {
        try {
            ProductSearchDocument searchDoc = mapToSearchDocument(product);
            searchRepository.save(searchDoc);
            log.info("Product indexed in Elasticsearch: {}", product.getProductId());
        } catch (Exception e) {
            log.error("Failed to index product: {}", product.getProductId(), e);
        }
    }

    /**
     * Remove product from Elasticsearch
     */
    public void removeFromIndex(String productId) {
        try {
            searchRepository.deleteById(productId);
            log.info("Product removed from Elasticsearch: {}", productId);
        } catch (Exception e) {
            log.error("Failed to remove product from index: {}", productId, e);
        }
    }

    /**
     * Re-index all products (maintenance operation)
     */
    public void reindexAll(Iterable<Product> products) {
        log.info("Starting full reindex...");
        int count = 0;
        for (Product product : products) {
            indexProduct(product);
            count++;
        }
        log.info("Reindexed {} products", count);
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
