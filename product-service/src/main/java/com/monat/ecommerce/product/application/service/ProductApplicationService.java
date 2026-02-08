package com.monat.ecommerce.product.application.service;

import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.product.application.dto.CreateProductRequest;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductSpecifications;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import com.monat.ecommerce.product.domain.service.ProductSyncService;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchDocument;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Product application service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductApplicationService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final ProductSyncService syncService;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product: {}", request.getProductId());

        // Check if product ID already exists
        if (productRepository.existsByProductId(request.getProductId())) {
            throw new IllegalArgumentException("Product ID already exists: " + request.getProductId());
        }

        // Build specifications
        ProductSpecifications specs = ProductSpecifications.builder()
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .color(request.getColor())
                .material(request.getMaterial())
                .additionalSpecs(request.getAdditionalSpecs() != null ? request.getAdditionalSpecs() : new HashMap<>())
                .build();

        // Create product
        Product product = Product.builder()
                .productId(request.getProductId())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .brand(request.getBrand())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .images(request.getImages())
                .tags(request.getTags())
                .specifications(specs)
                .status(ProductStatus.ACTIVE)
                .build();

        product = productRepository.save(product);

        // Index in Elasticsearch
        syncService.indexProduct(product);

        log.info("Product created successfully: {}", product.getProductId());

        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(String productId, CreateProductRequest request) {
        log.info("Updating product: {}", productId);

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        // Update fields
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency());
        product.setImages(request.getImages());
        product.setTags(request.getTags());

        // Update specifications
        ProductSpecifications specs = ProductSpecifications.builder()
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .color(request.getColor())
                .material(request.getMaterial())
                .additionalSpecs(request.getAdditionalSpecs() != null ? request.getAdditionalSpecs() : new HashMap<>())
                .build();
        product.setSpecifications(specs);

        product = productRepository.save(product);

        // Re-index in Elasticsearch
        syncService.indexProduct(product);

        log.info("Product updated successfully: {}", productId);

        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByStatus(ProductStatus status, Pageable pageable) {
        return productRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Transactional
    public void deleteProduct(String productId) {
        log.info("Deleting product: {}", productId);

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        productRepository.delete(product);
        syncService.removeFromIndex(product.getId());

        log.info("Product deleted: {}", productId);
    }

    /**
     * Search products using Elasticsearch
     */
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        log.debug("Searching products with keyword: {}", keyword);

        Page<ProductSearchDocument> results = searchRepository.searchByKeyword(keyword, pageable);
        return results.map(this::mapSearchDocToResponse);
    }

    /**
     * Search with filters
     */
    public Page<ProductResponse> searchWithFilters(
            String keyword,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        log.debug("Searching products - Keyword: {}, Category: {}, Price: {}-{}", 
                keyword, category, minPrice, maxPrice);

        if (keyword != null && minPrice != null && maxPrice != null) {
            return searchRepository.searchByKeywordAndPriceRange(keyword, minPrice, maxPrice, pageable)
                    .map(this::mapSearchDocToResponse);
        } else if (category != null && minPrice != null && maxPrice != null) {
            return searchRepository.findByCategoryAndPriceBetween(category, minPrice, maxPrice, pageable)
                    .map(this::mapSearchDocToResponse);
        } else if (keyword != null) {
            return searchRepository.searchByKeyword(keyword, pageable)
                    .map(this::mapSearchDocToResponse);
        } else if (category != null) {
            return searchRepository.findByCategory(category, pageable)
                    .map(this::mapSearchDocToResponse);
        } else {
            // Fallback to MongoDB
            return getAllProducts(pageable);
        }
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .currency(product.getCurrency())
                .images(product.getImages())
                .tags(product.getTags())
                .status(product.getStatus() != null ? product.getStatus().name() : null);

        if (product.getSpecifications() != null) {
            builder.weight(product.getSpecifications().getWeight())
                   .dimensions(product.getSpecifications().getDimensions())
                   .color(product.getSpecifications().getColor())
                   .material(product.getSpecifications().getMaterial())
                   .additionalSpecs(product.getSpecifications().getAdditionalSpecs());
        }

        return builder.build();
    }

    private ProductResponse mapSearchDocToResponse(ProductSearchDocument doc) {
        return ProductResponse.builder()
                .id(doc.getId())
                .productId(doc.getProductId())
                .name(doc.getName())
                .description(doc.getDescription())
                .category(doc.getCategory())
                .brand(doc.getBrand())
                .price(doc.getPrice())
                .tags(doc.getTags())
                .status(doc.getStatus())
                .build();
    }
}
