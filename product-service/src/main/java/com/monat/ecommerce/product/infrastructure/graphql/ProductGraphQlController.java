package com.monat.ecommerce.product.infrastructure.graphql;

import com.monat.ecommerce.product.application.service.ProductApplicationService;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductGraphQlInput;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductGraphQlModel;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductPageGraphQlModel;
import com.monat.ecommerce.product.infrastructure.graphql.mapper.ProductGraphQlMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Controller
@Validated
@RequiredArgsConstructor
public class ProductGraphQlController {

    private final ProductApplicationService productApplicationService;
    private final ProductGraphQlMapper productGraphQlMapper;

    @QueryMapping
    public ProductGraphQlModel product(@Argument("productId") String productId) {
        return productGraphQlMapper.toGraphQlModel(productApplicationService.getProduct(productId));
    }

    @QueryMapping
    public ProductPageGraphQlModel products(
            @Argument("page") Integer page,
            @Argument("size") Integer size,
            @Argument("sortBy") String sortBy) {
        return productGraphQlMapper.toPageModel(productApplicationService.getAllProducts(
                PageRequest.of(defaultPage(page), defaultSize(size), Sort.by(defaultSort(sortBy)))));
    }

    @QueryMapping
    public ProductPageGraphQlModel productsByCategory(
            @Argument("category") String category,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        return productGraphQlMapper.toPageModel(productApplicationService.getProductsByCategory(
                category,
                PageRequest.of(defaultPage(page), defaultSize(size))));
    }

    @QueryMapping
    public ProductPageGraphQlModel productsByStatus(
            @Argument("status") ProductStatus status,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        return productGraphQlMapper.toPageModel(productApplicationService.getProductsByStatus(
                status,
                PageRequest.of(defaultPage(page), defaultSize(size))));
    }

    @QueryMapping
    public ProductPageGraphQlModel searchProducts(
            @Argument("keyword") String keyword,
            @Argument("category") String category,
            @Argument("minPrice") BigDecimal minPrice,
            @Argument("maxPrice") BigDecimal maxPrice,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        return productGraphQlMapper.toPageModel(productApplicationService.searchWithFilters(
                keyword,
                category,
                minPrice,
                maxPrice,
                PageRequest.of(defaultPage(page), defaultSize(size))));
    }

    @MutationMapping
    public ProductGraphQlModel createProduct(@Argument("input") @Valid ProductGraphQlInput input) {
        return productGraphQlMapper.toGraphQlModel(
                productApplicationService.createProduct(productGraphQlMapper.toCreateRequest(input)));
    }

    @MutationMapping
    public ProductGraphQlModel updateProduct(
            @Argument("productId") String productId,
            @Argument("input") @Valid ProductGraphQlInput input) {
        return productGraphQlMapper.toGraphQlModel(
                productApplicationService.updateProduct(productId, productGraphQlMapper.toCreateRequest(input)));
    }

    @MutationMapping
    public Boolean deleteProduct(@Argument("productId") String productId) {
        productApplicationService.deleteProduct(productId);
        return Boolean.TRUE;
    }

    private int defaultPage(Integer page) {
        return page == null ? 0 : page;
    }

    private int defaultSize(Integer size) {
        return size == null ? 20 : size;
    }

    private String defaultSort(String sortBy) {
        return (sortBy == null || sortBy.isBlank()) ? "name" : sortBy;
    }
}
