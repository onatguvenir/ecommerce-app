package com.monat.ecommerce.cart.domain.model;

import com.monat.ecommerce.common.model.AbstractOrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * Cart item model stored in Redis.
 * Extends AbstractOrderItem to share common structure with OrderItem.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem extends com.monat.ecommerce.common.model.AbstractOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String imageUrl;

    @Override
    public void calculateSubtotal() {
        super.calculateSubtotal();
    }
}
