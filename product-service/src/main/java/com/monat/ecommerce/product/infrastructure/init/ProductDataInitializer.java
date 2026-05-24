package com.monat.ecommerce.product.infrastructure.init;

import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductSpecifications;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import com.monat.ecommerce.product.domain.service.ProductSyncService;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchDocument;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final ProductSyncService syncService;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(String... args) {
        ensureEsIndex();

        long mongoCount = productRepository.count();

        if (mongoCount == 0) {
            log.info("Initializing sample products...");
            productRepository.saveAll(sampleProducts());
            log.info("Saved {} products to MongoDB", productRepository.count());
            syncService.reindexAll();
            return;
        }

        long esCount = searchRepository.count();

        if (esCount == 0) {
            log.info("ES index empty, full reindex ({} products from MongoDB)...", mongoCount);
            syncService.reindexAll();
        } else if (esCount < mongoCount) {
            log.info("ES has {}/{} products, indexing {} missing...",
                    esCount, mongoCount, mongoCount - esCount);
            syncService.indexMissingProducts();
        } else {
            log.info("ES in sync ({} documents), skipping reindex.", esCount);
        }
    }

    /**
     * Creates the ES index with mapping only if it does not already exist.
     * Does NOT drop or recreate — preserves existing indexed data across restarts.
     * For mapping changes: drop the index manually and restart (triggers full reindex via esCount == 0 branch).
     */
    private void ensureEsIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductSearchDocument.class);
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping();
            log.info("Created 'products' Elasticsearch index with mapping.");
        }
    }

    private List<Product> sampleProducts() {
        return Arrays.asList(
                createProduct("PROD-001", "Laptop Pro 15", "High-performance laptop with 15-inch display",
                        "Electronics", "TechPro", new BigDecimal("1299.99"),
                        Arrays.asList("laptop", "computer", "electronics", "productivity"),
                        "2.1kg", "35.0 x 24.0 x 1.8 cm", "Silver", "Aluminum"),

                createProduct("PROD-002", "Wireless Mouse", "Ergonomic wireless mouse with precision tracking",
                        "Electronics", "TechPro", new BigDecimal("29.99"),
                        Arrays.asList("mouse", "wireless", "electronics", "accessories"),
                        "85g", "12.0 x 6.5 x 4.0 cm", "Black", "Plastic"),

                createProduct("PROD-003", "USB-C Cable 2m", "Premium USB-C cable with fast charging",
                        "Electronics", "TechPro", new BigDecimal("19.99"),
                        Arrays.asList("cable", "usb-c", "charging", "accessories"),
                        "50g", "200 cm", "Black", "Nylon Braided"),

                createProduct("PROD-004", "Mechanical Keyboard", "RGB mechanical keyboard with Cherry MX switches",
                        "Electronics", "TechPro", new BigDecimal("149.99"),
                        Arrays.asList("keyboard", "mechanical", "rgb", "gaming"),
                        "1.2kg", "44.0 x 13.0 x 4.0 cm", "Black", "Aluminum Frame"),

                createProduct("PROD-005", "Monitor 27\" 4K", "27-inch 4K UHD monitor with HDR support",
                        "Electronics", "TechPro", new BigDecimal("449.99"),
                        Arrays.asList("monitor", "4k", "display", "productivity"),
                        "5.8kg", "61.0 x 36.0 x 5.0 cm", "Black", "Plastic/Metal"),

                createProduct("PROD-006", "Webcam HD 1080p", "Full HD webcam with auto-focus and noise cancellation",
                        "Electronics", "TechPro", new BigDecimal("79.99"),
                        Arrays.asList("webcam", "camera", "streaming", "video-call"),
                        "150g", "9.0 x 7.0 x 7.0 cm", "Black", "Plastic"),

                createProduct("PROD-007", "Headphones Wireless", "Active noise cancellation wireless headphones",
                        "Electronics", "AudioMax", new BigDecimal("249.99"),
                        Arrays.asList("headphones", "wireless", "anc", "audio"),
                        "250g", "20.0 x 18.0 x 8.0 cm", "Black", "Premium Plastic"),

                createProduct("PROD-008", "Desk Lamp LED", "Adjustable LED desk lamp with USB charging port",
                        "Home", "LightWorks", new BigDecimal("39.99"),
                        Arrays.asList("lamp", "led", "desk", "lighting"),
                        "680g", "40.0 x 15.0 x 8.0 cm", "White", "Aluminum/Plastic"),

                createProduct("PROD-009", "Notebook A5", "Premium leather-bound notebook with 200 pages",
                        "Stationery", "PaperPlus", new BigDecimal("14.99"),
                        Arrays.asList("notebook", "stationery", "writing"),
                        "320g", "21.0 x 14.8 x 1.5 cm", "Brown", "Leather/Paper"),

                createProduct("PROD-010", "Pen Set Premium", "Set of 5 premium ballpoint pens",
                        "Stationery", "WritePro", new BigDecimal("24.99"),
                        Arrays.asList("pen", "stationery", "writing", "office"),
                        "100g", "15.0 x 10.0 x 2.0 cm", "Mixed", "Metal")
        );
    }

    private Product createProduct(String productId, String name, String description,
                                   String category, String brand, BigDecimal price,
                                   List<String> tags,
                                   String weight, String dimensions, String color, String material) {
        ProductSpecifications specs = ProductSpecifications.builder()
                .weight(weight)
                .dimensions(dimensions)
                .color(color)
                .material(material)
                .build();

        return Product.builder()
                .productId(productId)
                .name(name)
                .description(description)
                .category(category)
                .brand(brand)
                .price(price)
                .currency("USD")
                .tags(tags)
                .specifications(specs)
                .status(ProductStatus.ACTIVE)
                .build();
    }
}
