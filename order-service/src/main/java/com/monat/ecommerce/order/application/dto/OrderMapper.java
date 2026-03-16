package com.monat.ecommerce.order.application.dto;

import com.monat.ecommerce.order.domain.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "currency", constant = "USD")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "shippingAddress", source = "shippingAddress")
    Order toOrder(CreateOrderRequest request);

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    OrderResponse toOrderResponse(Order order);

    List<OrderResponse> toOrderResponseList(List<Order> orders);

    OrderItemResponse toOrderItemResponse(OrderItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    OrderItem toOrderItem(OrderItemRequest request);

    AddressResponse toAddressResponse(ShippingAddress address);

    @Mapping(target = "street", source = "street")
    @Mapping(target = "city", source = "city")
    ShippingAddress toShippingAddress(AddressRequest request);
}
