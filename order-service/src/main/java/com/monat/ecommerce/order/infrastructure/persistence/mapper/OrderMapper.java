package com.monat.ecommerce.order.infrastructure.persistence.mapper;

import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderItem;
import com.monat.ecommerce.order.domain.model.ShippingAddress;
import com.monat.ecommerce.order.infrastructure.persistence.entity.OrderEntity;
import com.monat.ecommerce.order.infrastructure.persistence.entity.OrderItemEntity;
import com.monat.ecommerce.order.infrastructure.persistence.entity.ShippingAddressEmbeddable;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public Order toDomain(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        Order order = Order.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .userId(entity.getUserId() != null ? entity.getUserId().toString() : null)
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .paymentReference(entity.getPaymentReference())
                .cancellationReason(entity.getCancellationReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .items(toDomainItems(entity.getItems()))
                .build();

        if (entity.getShippingAddress() != null) {
            order.setShippingAddress(ShippingAddress.builder()
                    .street(entity.getShippingAddress().getStreet())
                    .city(entity.getShippingAddress().getCity())
                    .state(entity.getShippingAddress().getState())
                    .postalCode(entity.getShippingAddress().getPostalCode())
                    .country(entity.getShippingAddress().getCountry())
                    .build());
        }

        return order;
    }

    public OrderEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }

        OrderEntity entity = OrderEntity.builder()
                .id(domain.getId() != null ? domain.getId() : UUID.randomUUID())
                .orderNumber(domain.getOrderNumber())
                .userId(domain.getUserId() != null ? UUID.fromString(domain.getUserId()) : null)
                .status(domain.getStatus())
                .totalAmount(domain.getTotalAmount())
                .currency(domain.getCurrency())
                .paymentReference(domain.getPaymentReference())
                .cancellationReason(domain.getCancellationReason())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();

        if (domain.getShippingAddress() != null) {
            entity.setShippingAddress(ShippingAddressEmbeddable.builder()
                    .street(domain.getShippingAddress().getStreet())
                    .city(domain.getShippingAddress().getCity())
                    .state(domain.getShippingAddress().getState())
                    .postalCode(domain.getShippingAddress().getPostalCode())
                    .country(domain.getShippingAddress().getCountry())
                    .build());
        }

        // Handle bidirectional relationship
        if (domain.getItems() != null) {
            List<OrderItemEntity> itemEntities = domain.getItems().stream()
                    .map(item -> {
                        OrderItemEntity itemEntity = toEntityItem(item);
                        itemEntity.setOrder(entity);
                        return itemEntity;
                    })
                    .collect(Collectors.toList());
            entity.setItems(itemEntities);
        }

        return entity;
    }

    private List<OrderItem> toDomainItems(List<OrderItemEntity> itemEntities) {
        if (itemEntities == null) {
            return Collections.emptyList();
        }
        return itemEntities.stream()
                .map(this::toDomainItem)
                .collect(Collectors.toList());
    }

    private OrderItem toDomainItem(OrderItemEntity entity) {
        return OrderItem.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .subtotal(entity.getSubtotal())
                .build();
    }

    private OrderItemEntity toEntityItem(OrderItem domain) {
        return OrderItemEntity.builder()
                .id(domain.getId() != null ? domain.getId() : UUID.randomUUID())
                .productId(domain.getProductId())
                .productName(domain.getProductName())
                .quantity(domain.getQuantity())
                .unitPrice(domain.getUnitPrice())
                .subtotal(domain.getSubtotal())
                .build();
    }
}
