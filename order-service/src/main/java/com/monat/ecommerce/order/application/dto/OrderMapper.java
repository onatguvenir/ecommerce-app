package com.monat.ecommerce.order.application.dto;

import com.monat.ecommerce.order.domain.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    OrderResponse toOrderResponse(Order order);

    List<OrderResponse> toOrderResponseList(List<Order> orders);

    OrderItemResponse toOrderItemResponse(OrderItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    OrderItem toOrderItem(OrderItemRequest request);

    AddressResponse toAddressResponse(ShippingAddress address);

    @Mapping(target = "street", source = "street")
    @Mapping(target = "city", source = "city")
    ShippingAddress toShippingAddress(AddressRequest request);
}
