package com.monat.ecommerce.inventory.application.dto;

import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.model.StockReservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryMapper {

    @Mapping(target = "quantity", source = "totalQuantity")
    @Mapping(target = "availableQuantity", source = "availableQuantity")
    @Mapping(target = "reservedQuantity", source = "reservedQuantity")
    InventoryResponse toResponse(Inventory inventory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservationId", ignore = true) // Generated in service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    StockReservation toReservation(StockReservationRequest request);
}
