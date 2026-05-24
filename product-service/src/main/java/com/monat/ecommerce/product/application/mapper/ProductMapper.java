package com.monat.ecommerce.product.application.mapper;

import com.monat.ecommerce.product.application.command.CreateProductCommand;
import com.monat.ecommerce.product.application.command.UpdateProductCommand;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductSpecifications;
import com.monat.ecommerce.product.infrastructure.search.ProductSearchDocument;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "specifications", source = "command")
    Product toEntity(CreateProductCommand command);

    @Mapping(target = "specifications", source = "command")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", ignore = true) // Product ID should not be updated usually
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromCommand(UpdateProductCommand command, @MappingTarget Product product);

    @Mapping(target = "weight", source = "specifications.weight")
    @Mapping(target = "dimensions", source = "specifications.dimensions")
    @Mapping(target = "color", source = "specifications.color")
    @Mapping(target = "material", source = "specifications.material")
    @Mapping(target = "additionalSpecs", source = "specifications.additionalSpecs")
    ProductResponse toResponse(Product product);

    @Mapping(target = "additionalSpecs", source = "additionalSpecs")
    ProductSpecifications toSpecifications(CreateProductCommand command);

    @Mapping(target = "additionalSpecs", source = "additionalSpecs")
    ProductSpecifications toSpecifications(UpdateProductCommand command);

    @Mapping(target = "images", expression = "java(java.util.List.of())")
    @Mapping(target = "additionalSpecs", expression = "java(java.util.Map.of())")
    ProductResponse toResponse(ProductSearchDocument doc);
}
