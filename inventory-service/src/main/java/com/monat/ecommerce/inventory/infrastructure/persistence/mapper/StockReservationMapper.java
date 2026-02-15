package com.monat.ecommerce.inventory.infrastructure.persistence.mapper;

import com.monat.ecommerce.inventory.domain.model.StockReservation;
import com.monat.ecommerce.inventory.infrastructure.persistence.entity.StockReservationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockReservationMapper {

    StockReservation toDomain(StockReservationEntity entity);

    StockReservationEntity toEntity(StockReservation domain);
}
