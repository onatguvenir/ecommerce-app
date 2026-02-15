package com.monat.ecommerce.inventory.infrastructure.persistence.mapper;

import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.infrastructure.persistence.entity.InventoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InventoryMapper {

    Inventory toDomain(InventoryEntity entity);

    InventoryEntity toEntity(Inventory domain);
}
