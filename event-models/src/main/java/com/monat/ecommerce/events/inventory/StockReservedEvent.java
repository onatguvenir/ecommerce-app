package com.monat.ecommerce.events.inventory;

import com.monat.ecommerce.events.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Event published when stock is reserved for an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent extends BaseEvent {
    private String reservationId;
    private String orderId;
    private List<ReservedItem> items;
    private boolean allReserved;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedItem {
        private String productId;
        private Integer quantity;
        private String warehouseLocation;
    }
}
