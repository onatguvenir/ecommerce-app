package com.monat.ecommerce.product.infrastructure.graphql;

import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.service.ProductApplicationService;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductGraphQlInput;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductGraphQlModel;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductPageGraphQlModel;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductSpecificationEntry;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductSpecificationEntryInput;
import com.monat.ecommerce.product.infrastructure.graphql.mapper.ProductGraphQlMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductGraphQlController Tests")
class ProductGraphQlControllerTest {

    @Mock
    private ProductApplicationService productApplicationService;

    @InjectMocks
    private ProductGraphQlController productGraphQlController;

    private ProductGraphQlInput productInput;

    @BeforeEach
    void setUp() {
        productGraphQlController = new ProductGraphQlController(
                productApplicationService,
                new ProductGraphQlMapper());

        productInput = new ProductGraphQlInput(
                "PROD-001",
                "GraphQL Product",
                "GraphQL product description",
                "Electronics",
                "Monat",
                new BigDecimal("49.99"),
                "USD",
                List.of("image-1"),
                List.of("featured"),
                "1kg",
                "10x10x10",
                "Black",
                "Aluminum",
                List.of(new ProductSpecificationEntryInput("memory", "16GB")));
    }

    @Test
    @DisplayName("product query should return a mapped product")
    void productQuery_ShouldReturnProduct() {
        when(productApplicationService.getProduct("PROD-001")).thenReturn(sampleProduct());

        ProductGraphQlModel response = productGraphQlController.product("PROD-001");

        assertThat(response.productId()).isEqualTo("PROD-001");
        assertThat(response.name()).isEqualTo("GraphQL Product");
        assertThat(response.additionalSpecs())
                .containsExactly(new ProductSpecificationEntry("memory", "16GB"));
        verify(productApplicationService).getProduct("PROD-001");
    }

    @Test
    @DisplayName("productsByStatus query should return paged content")
    void productsByStatus_ShouldReturnPage() {
        when(productApplicationService.getProductsByStatus(eq(ProductStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(sampleProduct())));

        ProductPageGraphQlModel response = productGraphQlController.productsByStatus(ProductStatus.ACTIVE, 0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.content().getFirst().productId()).isEqualTo("PROD-001");
        verify(productApplicationService).getProductsByStatus(eq(ProductStatus.ACTIVE), any(PageRequest.class));
    }

    @Test
    @DisplayName("createProduct mutation should delegate to application service")
    void createProduct_ShouldDelegateToApplicationService() {
        when(productApplicationService.createProduct(any())).thenReturn(sampleProduct());

        ProductGraphQlModel response = productGraphQlController.createProduct(productInput);

        assertThat(response.productId()).isEqualTo("PROD-001");
        assertThat(response.price()).isEqualByComparingTo("49.99");
        verify(productApplicationService).createProduct(any());
    }

    @Test
    @DisplayName("deleteProduct mutation should delegate and return true")
    void deleteProduct_ShouldReturnTrue() {
        Boolean response = productGraphQlController.deleteProduct("PROD-001");

        assertThat(response).isTrue();
        verify(productApplicationService).deleteProduct("PROD-001");
    }

    private ProductResponse sampleProduct() {
        return ProductResponse.builder()
                .id("mongo-1")
                .productId("PROD-001")
                .name("GraphQL Product")
                .description("GraphQL product description")
                .category("Electronics")
                .brand("Monat")
                .price(new BigDecimal("49.99"))
                .currency("USD")
                .images(List.of("image-1"))
                .tags(List.of("featured"))
                .status("ACTIVE")
                .weight("1kg")
                .dimensions("10x10x10")
                .color("Black")
                .material("Aluminum")
                .additionalSpecs(Map.of("memory", "16GB"))
                .build();
    }
}
