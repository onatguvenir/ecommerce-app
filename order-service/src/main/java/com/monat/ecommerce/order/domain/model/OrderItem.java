package com.monat.ecommerce.order.domain.model;

import com.monat.ecommerce.common.model.AbstractOrderItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Order item value object/entity in domain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem extends AbstractOrderItem {

    private UUID id;

    public void calculateSubtotal() {
        super.calculateSubtotal();
    }
}
