package com.monat.ecommerce.product.application.service;

import com.monat.ecommerce.product.application.command.CreateProductCommand;
import com.monat.ecommerce.product.application.command.DeleteProductCommand;
import com.monat.ecommerce.product.application.command.ProductCommandService;
import com.monat.ecommerce.product.application.command.UpdateProductCommand;
import com.monat.ecommerce.product.application.dto.CreateProductRequest;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.query.ProductQueryService;
import com.monat.ecommerce.product.application.query.SearchProductsQuery;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Product Application Service — CQRS Facade (Backward Compatibility Layer).
 * <p>
 * Bu sınıf artık iş mantığı içermez. Sadece:
 * 1. Eski DTO'ları (CreateProductRequest) yeni Command/Query nesnelerine
 * dönüştürür.
 * 2. ProductCommandService (write) ve ProductQueryService (read) arasında köprü
 * kurar.
 * <p>
 * Neden korunuyor?
 * - Geriye dönük uyumluluk: Mevcut controller bu servisi kullanıyor.
 * - Kademeli geçiş: Controller doğrudan CQRS servislerine geçirildiğinde bu
 * sınıf kaldırılabilir.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductApplicationService {

    private final ProductCommandService commandService;
    private final ProductQueryService queryService;

    // ---- Write Operations (Command Side) ----

    public ProductResponse createProduct(CreateProductRequest request) {
        CreateProductCommand command = new CreateProductCommand(
                request.productId(), request.name(), request.description(),
                request.category(), request.brand(), request.price(),
                request.currency(), request.images(), request.tags(),
                request.weight(), request.dimensions(), request.color(),
                request.material(), request.additionalSpecs());
        return commandService.createProduct(command);
    }

    public ProductResponse updateProduct(String productId, CreateProductRequest request) {
        UpdateProductCommand command = new UpdateProductCommand(
                productId, request.name(), request.description(),
                request.category(), request.brand(), request.price(),
                request.currency(), request.images(), request.tags(),
                request.weight(), request.dimensions(), request.color(),
                request.material(), request.additionalSpecs());
        return commandService.updateProduct(command);
    }

    public void deleteProduct(String productId) {
        commandService.deleteProduct(new DeleteProductCommand(productId));
    }

    // ---- Read Operations (Query Side) ----

    public ProductResponse getProduct(String productId) {
        return queryService.getProduct(productId);
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return queryService.getAllProducts(pageable);
    }

    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        return queryService.getProductsByCategory(category, pageable);
    }

    public Page<ProductResponse> getProductsByStatus(ProductStatus status, Pageable pageable) {
        return queryService.getProductsByStatus(status, pageable);
    }

    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return queryService.searchProducts(new SearchProductsQuery(keyword, null, null, null, null, pageable));
    }

    public Page<ProductResponse> searchWithFilters(
            String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return queryService.searchProducts(
                new SearchProductsQuery(keyword, category, null, minPrice, maxPrice, pageable));
    }
}
