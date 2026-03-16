package com.monat.ecommerce.product.application.service;

import com.monat.ecommerce.product.application.command.ProductCommandService;
import com.monat.ecommerce.product.application.dto.CreateProductRequest;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.query.ProductQueryService;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProductApplicationService Unit Tests (CQRS Delegation Layer).
 * <p>
 * Bu test sınıfı, ProductApplicationService'in doğru şekilde
 * ProductCommandService ve ProductQueryService'e delege ettiğini doğrular.
 * <p>
 * Artık ProductSyncService ve ProductSearchRepository mock'lanmıyor —
 * bu sorumluluklar handler'lara ve event listener'lara taşındı.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductApplicationService (CQRS Delegation) Tests")
class ProductApplicationServiceTest {

    @Mock
    private ProductCommandService commandService;

    @Mock
    private ProductQueryService queryService;

    @InjectMocks
    private ProductApplicationService productApplicationService;

    private CreateProductRequest createProductRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        createProductRequest = CreateProductRequest.builder()
                .productId("PROD-001")
                .name("Test Product")
                .description("A detailed test product description")
                .price(BigDecimal.valueOf(99.99))
                .category("Electronics")
                .brand("TestBrand")
                .build();

        productResponse = ProductResponse.builder()
                .id("mongo-id-001")
                .productId("PROD-001")
                .name("Test Product")
                .description("A detailed test product description")
                .price(BigDecimal.valueOf(99.99))
                .category("Electronics")
                .brand("TestBrand")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("createProduct: CreateProductCommand oluşturulup CommandService'e delege edilmeli")
    void createProduct_ShouldDelegateToCommandService() {
        // Given
        when(commandService.createProduct(any())).thenReturn(productResponse);

        // When
        ProductResponse response = productApplicationService.createProduct(createProductRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo("PROD-001");
        assertThat(response.name()).isEqualTo("Test Product");
        verify(commandService).createProduct(any());
        verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("updateProduct: UpdateProductCommand oluşturulup CommandService'e delege edilmeli")
    void updateProduct_ShouldDelegateToCommandService() {
        // Given
        when(commandService.updateProduct(any())).thenReturn(productResponse);

        // When
        ProductResponse response = productApplicationService.updateProduct("PROD-001", createProductRequest);

        // Then
        assertThat(response).isNotNull();
        verify(commandService).updateProduct(any());
    }

    @Test
    @DisplayName("deleteProduct: DeleteProductCommand oluşturulup CommandService'e delege edilmeli")
    void deleteProduct_ShouldDelegateToCommandService() {
        // Given
        doNothing().when(commandService).deleteProduct(any());

        // When
        productApplicationService.deleteProduct("PROD-001");

        // Then
        verify(commandService).deleteProduct(any());
        verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("getProduct: GetProductQuery oluşturulup QueryService'e delege edilmeli")
    void getProduct_ShouldDelegateToQueryService() {
        // Given
        when(queryService.getProduct("PROD-001")).thenReturn(productResponse);

        // When
        ProductResponse response = productApplicationService.getProduct("PROD-001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo("PROD-001");
        verify(queryService).getProduct("PROD-001");
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("getProduct: Ürün bulunamazsa ResourceNotFoundException yayılmalı")
    void getProduct_NotFound_ShouldPropagateException() {
        // Given
        when(queryService.getProduct("INVALID"))
                .thenThrow(new ResourceNotFoundException("Product not found: INVALID"));

        // When & Then
        assertThatThrownBy(() -> productApplicationService.getProduct("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("getAllProducts: QueryService'e delege edilmeli")
    void getAllProducts_ShouldDelegateToQueryService() {
        // Given
        Page<ProductResponse> page = new PageImpl<>(List.of(productResponse));
        when(queryService.getAllProducts(any(PageRequest.class))).thenReturn(page);

        // When
        Page<ProductResponse> results = productApplicationService.getAllProducts(PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        verify(queryService).getAllProducts(any(PageRequest.class));
    }

    @Test
    @DisplayName("getProductsByCategory: QueryService'e delege edilmeli")
    void getProductsByCategory_ShouldDelegateToQueryService() {
        // Given
        Page<ProductResponse> page = new PageImpl<>(List.of(productResponse));
        when(queryService.getProductsByCategory(eq("Electronics"), any(PageRequest.class))).thenReturn(page);

        // When
        Page<ProductResponse> results = productApplicationService.getProductsByCategory(
                "Electronics", PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent().get(0).category()).isEqualTo("Electronics");
        verify(queryService).getProductsByCategory(eq("Electronics"), any(PageRequest.class));
    }
}
