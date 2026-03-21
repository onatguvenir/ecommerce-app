package com.monat.ecommerce.product.application.command.handler;

import com.monat.ecommerce.product.application.command.CreateProductCommand;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.domain.event.ProductCreatedEvent;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CreateProductCommandHandler Unit Tests.
 * <p>
 * Test Stratejisi:
 * - @ExtendWith(MockitoExtension.class): Spring context başlatmadan hızlı unit
 * test.
 * - Tüm bağımlılıklar mock'lanır: gerçek MongoDB ve ES bağlantısı gerekmez.
 * - ArgumentCaptor: Publish edilen event'in içeriğini doğrular.
 * - Given-When-Then (Arrange-Act-Assert) pattern kullanılır.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductCommandHandler Tests")
class CreateProductCommandHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateProductCommandHandler handler;

    private CreateProductCommand validCommand;
    private Product savedProduct;

    @BeforeEach
    void setUp() {
        validCommand = new CreateProductCommand(
                "PROD-001", "Test Product", "A detailed test product description",
                "Electronics", "TestBrand", BigDecimal.valueOf(99.99),
                "USD", List.of("img1.jpg"), List.of("tag1"),
                "1kg", "10x10x10", "Black", "Plastic", null);

        savedProduct = Product.builder()
                .id("mongo-id-001")
                .productId("PROD-001")
                .name("Test Product")
                .description("A detailed test product description")
                .category("Electronics")
                .brand("TestBrand")
                .price(BigDecimal.valueOf(99.99))
                .currency("USD")
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Geçerli command ile ürün başarıyla oluşturulmalı")
    void handle_ValidCommand_ShouldCreateProductAndPublishEvent() {
        // Given
        when(productRepository.existsByProductId("PROD-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        ProductResponse response = handler.handle(validCommand);

        // Then — response doğrulama
        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo("PROD-001");
        assertThat(response.name()).isEqualTo("Test Product");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(response.status()).isEqualTo("ACTIVE");

        // Then — MongoDB'ye kayıt doğrulama
        verify(productRepository).existsByProductId("PROD-001");
        verify(productRepository).save(any(Product.class));

        // Then — Domain event yayınlandı mı?
        ArgumentCaptor<ProductCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ProductCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getProduct().getProductId()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("Duplicate productId ile oluşturma girişimi IllegalArgumentException fırlatmalı")
    void handle_DuplicateProductId_ShouldThrowIllegalArgumentException() {
        // Given
        when(productRepository.existsByProductId("PROD-001")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> handler.handle(validCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product ID already exists: PROD-001");

        // MongoDB'ye kayıt yapılmamalı
        verify(productRepository, never()).save(any());
        // Event yayınlanmamalı
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Currency null ise default 'USD' atanmalı")
    void handle_NullCurrency_ShouldDefaultToUSD() {
        // Given — currency null olan command
        CreateProductCommand commandWithNullCurrency = new CreateProductCommand(
                "PROD-002", "Test Product", "A detailed test product description",
                "Electronics", "TestBrand", BigDecimal.valueOf(99.99),
                null, null, null, null, null, null, null, null);

        Product productWithUSD = Product.builder()
                .id("mongo-id-002").productId("PROD-002").name("Test Product")
                .price(BigDecimal.valueOf(99.99)).currency("USD").status(ProductStatus.ACTIVE).build();

        when(productRepository.existsByProductId("PROD-002")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(productWithUSD);

        // When
        handler.handle(commandWithNullCurrency);

        // Then — save edilen Product'ın currency'si USD olmalı
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getCurrency()).isEqualTo("USD");
    }
}
