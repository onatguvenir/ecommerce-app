package com.monat.ecommerce.product.application.service;

import com.monat.ecommerce.product.application.dto.CreateProductRequest;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchRepository;
import com.monat.ecommerce.product.domain.service.ProductSyncService;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductApplicationService
 */
@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private ProductSyncService productSyncService;

    @InjectMocks
    private ProductApplicationService productApplicationService;

    private CreateProductRequest createProductRequest;
    private Product product;

    @BeforeEach
    void setUp() {
        createProductRequest = CreateProductRequest.builder()
                .productId("PROD-001")
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(99.99))
                .category("Electronics")
                .brand("TestBrand")
                .build();

        product = Product.builder()
                .id("mongo-id-001")
                .productId("PROD-001")
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(99.99))
                .category("Electronics")
                .brand("TestBrand")
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void createProduct_Success() {
        // Given
        when(productRepository.existsByProductId("PROD-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductResponse response = productApplicationService.createProduct(createProductRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        verify(productRepository, times(1)).existsByProductId("PROD-001");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getProduct_Found() {
        // Given
        when(productRepository.findByProductId("PROD-001")).thenReturn(Optional.of(product));

        // When
        ProductResponse response = productApplicationService.getProduct("PROD-001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo("PROD-001");
        assertThat(response.getName()).isEqualTo("Test Product");
        verify(productRepository, times(1)).findByProductId("PROD-001");
    }

    @Test
    void getProduct_NotFound() {
        // Given
        when(productRepository.findByProductId("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productApplicationService.getProduct("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository, times(1)).findByProductId("INVALID");
    }

    @Test
    void updateProduct_Success() {
        // Given
        when(productRepository.findByProductId("PROD-001")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        CreateProductRequest updateRequest = CreateProductRequest.builder()
                .productId("PROD-001")
                .name("Updated Product")
                .description("Updated Description")
                .price(BigDecimal.valueOf(149.99))
                .category("Electronics")
                .brand("TestBrand")
                .build();

        // When
        ProductResponse response = productApplicationService.updateProduct("PROD-001", updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(productRepository, times(1)).findByProductId("PROD-001");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void deleteProduct_Success() {
        // Given
        when(productRepository.findByProductId("PROD-001")).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(any(Product.class));

        // When
        productApplicationService.deleteProduct("PROD-001");

        // Then
        verify(productRepository, times(1)).findByProductId("PROD-001");
        verify(productRepository, times(1)).delete(product);
    }

    @Test
    void searchProducts_Success() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(productPage);

        // When
        Page<ProductResponse> results = productApplicationService.getAllProducts(PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        verify(productRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void getAllProducts_Success() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(productPage);

        // When
        Page<ProductResponse> results = productApplicationService.getAllProducts(PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        verify(productRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void getProductsByCategory_Success() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.findByCategory(anyString(), any(PageRequest.class)))
                .thenReturn(productPage);

        // When
        Page<ProductResponse> results = productApplicationService.getProductsByCategory(
                "Electronics", PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getCategory()).isEqualTo("Electronics");
        verify(productRepository, times(1)).findByCategory("Electronics", PageRequest.of(0, 10));
    }
}
