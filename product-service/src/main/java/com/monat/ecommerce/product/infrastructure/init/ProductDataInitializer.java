package com.monat.ecommerce.product.infrastructure.init;

import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductSpecifications;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import com.monat.ecommerce.product.domain.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Initialize sample product data on startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductSyncService syncService;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Products already initialized, skipping...");
            return;
        }

        log.info("Initializing sample products...");

        List<Product> products = Arrays.asList(
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

        productRepository.saveAll(products);
        log.info("Saved {} products to MongoDB", products.size());

        // Index in Elasticsearch
        syncService.reindexAll(products);

        log.info("Product initialization complete!");
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
