package com.monat.ecommerce.product.infrastructure.persistence.mapper;

import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.infrastructure.persistence.document.ProductDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        implementationName = "ProductPersistenceMapperImpl"
)
public interface ProductMapper {

    Product toDomain(ProductDocument document);

    ProductDocument toDocument(Product domain);
}
