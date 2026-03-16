package com.monat.ecommerce.product.infrastructure.bootstrap;

import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * Seeds the database with dummy products.
 * Only runs when 'docker' profile is active and database is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class ProductDataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() > 0) {
            log.info("Products already exist. Skipping seeding.");
            return;
        }

        log.info("Seeding products...");

        // Electronics
        createProduct("PROD-001", "Smartphone X Pro", "High-end smartphone with advanced camera system.", "Electronics",
                "TechBrand", new BigDecimal("1000.00"));
        createProduct("PROD-002", "Laptop Ultra 15", "Ultra-thin laptop for professionals.", "Electronics", "ComputeCo",
                new BigDecimal("1500.00"));
        createProduct("PROD-003", "Wireless Earbuds", "Noise-cancelling wireless earbuds.", "Electronics", "AudioTech",
                new BigDecimal("200.00"));
        createProduct("PROD-004", "Smart Watch Series 5", "Fitness tracker and smartwatch.", "Electronics", "TechBrand",
                new BigDecimal("300.00"));
        createProduct("PROD-005", "4K Monitor 27\"", "High-resolution monitor for designers.", "Electronics",
                "ViewMaster", new BigDecimal("450.00"));

        // Books
        createProduct("PROD-006", "The Great Novel", "Award-winning fiction novel.", "Books", "PublisherOne",
                new BigDecimal("20.00"));
        createProduct("PROD-007", "Learn Java 21", "Comprehensive guide to Java programming.", "Books", "TechPress",
                new BigDecimal("50.00"));
        createProduct("PROD-008", "Cooking Masterclass", "Recipes from top chefs.", "Books", "CulinaryPub",
                new BigDecimal("30.00"));
        createProduct("PROD-009", "History of Art", "Illustrated history of art movements.", "Books", "ArtHouse",
                new BigDecimal("60.00"));
        createProduct("PROD-010", "Sci-Fi Adventure", "Thrilling space opera.", "Books", "GalaxyPress",
                new BigDecimal("15.00"));

        // Clothing
        createProduct("PROD-011", "Classic T-Shirt", "100% Cotton T-Shirt.", "Clothing", "FashionBasic",
                new BigDecimal("25.00"));
        createProduct("PROD-012", "Denim Jeans", "Straight-fit denim jeans.", "Clothing", "BlueJeans",
                new BigDecimal("80.00"));
        createProduct("PROD-013", "Running Shoes", "Lightweight running shoes.", "Clothing", "RunFast",
                new BigDecimal("120.00"));
        createProduct("PROD-014", "Winter Jacket", "Insulated winter jacket.", "Clothing", "NorthWear",
                new BigDecimal("200.00"));
        createProduct("PROD-015", "Summer Dress", "Floral print summer dress.", "Clothing", "StyleCo",
                new BigDecimal("60.00"));

        // Home
        createProduct("PROD-016", "Coffee Maker", "Automatic drip coffee maker.", "Home", "BrewMaster",
                new BigDecimal("90.00"));
        createProduct("PROD-017", "Blender", "High-speed blender for smoothies.", "Home", "KitchenPro",
                new BigDecimal("130.00"));
        createProduct("PROD-018", "Desk Lamp", "Adjustable LED desk lamp.", "Home", "LightUp", new BigDecimal("40.00"));
        createProduct("PROD-019", "Throw Pillow", "Decorative throw pillow.", "Home", "DecoHome",
                new BigDecimal("20.00"));
        createProduct("PROD-020", "Plant Pot", "Ceramic plant pot.", "Home", "GardenLife", new BigDecimal("25.00"));

        log.info("Seeding products completed. Created {} products.", productRepository.count());
    }

    private void createProduct(String productId, String name, String description, String category, String brand,
            BigDecimal price) {
        Product product = Product.builder()
                .productId(productId)
                .name(name)
                .description(description)
                .category(category)
                .brand(brand)
                .price(price)
                .currency("USD")
                .status(ProductStatus.ACTIVE)
                .images(Arrays.asList("https://via.placeholder.com/150?text=" + name.replaceAll(" ", "+")))
                .tags(Arrays.asList(category.toLowerCase(), "new"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0L)
                .build();

        productRepository.save(product);
    }
}
