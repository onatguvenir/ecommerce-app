package com.monat.ecommerce.product.infrastructure.graphql.mapper;

import com.monat.ecommerce.product.application.dto.CreateProductRequest;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductGraphQlInput;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductGraphQlModel;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductPageGraphQlModel;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductSpecificationEntry;
import com.monat.ecommerce.product.infrastructure.graphql.dto.ProductSpecificationEntryInput;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ProductGraphQlMapper {

    public CreateProductRequest toCreateRequest(ProductGraphQlInput input) {
        return CreateProductRequest.builder()
                .productId(input.productId())
                .name(input.name())
                .description(input.description())
                .category(input.category())
                .brand(input.brand())
                .price(input.price())
                .currency(input.currency())
                .images(input.images())
                .tags(input.tags())
                .weight(input.weight())
                .dimensions(input.dimensions())
                .color(input.color())
                .material(input.material())
                .additionalSpecs(toSpecificationMap(input.additionalSpecs()))
                .build();
    }

    public ProductGraphQlModel toGraphQlModel(ProductResponse response) {
        return new ProductGraphQlModel(
                response.id(),
                response.productId(),
                response.name(),
                response.description(),
                response.category(),
                response.brand(),
                response.price(),
                response.currency(),
                response.images(),
                response.tags(),
                response.status(),
                response.weight(),
                response.dimensions(),
                response.color(),
                response.material(),
                toSpecificationEntries(response.additionalSpecs()));
    }

    public ProductPageGraphQlModel toPageModel(Page<ProductResponse> page) {
        return new ProductPageGraphQlModel(
                page.getContent().stream().map(this::toGraphQlModel).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    private Map<String, String> toSpecificationMap(List<ProductSpecificationEntryInput> additionalSpecs) {
        if (additionalSpecs == null || additionalSpecs.isEmpty()) {
            return Map.of();
        }
        return additionalSpecs.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(
                        ProductSpecificationEntryInput::key,
                        ProductSpecificationEntryInput::value,
                        (first, second) -> second,
                        java.util.LinkedHashMap::new));
    }

    private List<ProductSpecificationEntry> toSpecificationEntries(Map<String, String> additionalSpecs) {
        if (additionalSpecs == null || additionalSpecs.isEmpty()) {
            return List.of();
        }
        return additionalSpecs.entrySet().stream()
                .map(entry -> new ProductSpecificationEntry(entry.getKey(), entry.getValue()))
                .toList();
    }
}
