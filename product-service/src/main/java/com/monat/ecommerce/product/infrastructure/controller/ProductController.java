package com.monat.ecommerce.product.infrastructure.controller;

import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.product.application.dto.CreateProductRequest;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.service.ProductApplicationService;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Product catalog operations")
public class ProductController {

    private final ProductApplicationService productService;

    @PostMapping
    @Operation(summary = "Create new product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse response = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Product created successfully")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse response = productService.updateProduct(productId, request);

        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .success(true)
                .data(response)
                .message("Product updated successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable String productId) {
        ProductResponse response = productService.getProduct(productId);

        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .success(true)
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<ProductResponse> products = productService.getAllProducts(pageable);

        return ResponseEntity.ok(PagedResponse.<ProductResponse>builder()
                .content(products.getContent())
                .page(products.getNumber())
                .size(products.getSize())
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .last(products.isLast())
                .build());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<PagedResponse<ProductResponse>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getProductsByCategory(category, pageable);

        return ResponseEntity.ok(PagedResponse.<ProductResponse>builder()
                .content(products.getContent())
                .page(products.getNumber())
                .size(products.getSize())
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .last(products.isLast())
                .build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search products using Elasticsearch", 
               description = "Full-text search across name, description, category, and brand")
    public ResponseEntity<PagedResponse<ProductResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.searchWithFilters(
                keyword, category, minPrice, maxPrice, pageable);

        return ResponseEntity.ok(PagedResponse.<ProductResponse>builder()
                .content(products.getContent())
                .page(products.getNumber())
                .size(products.getSize())
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .last(products.isLast())
                .build());
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String productId) {
        productService.deleteProduct(productId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Product deleted successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
