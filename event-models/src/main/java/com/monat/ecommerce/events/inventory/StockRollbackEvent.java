package com.monat.ecommerce.events.inventory;

import com.monat.ecommerce.events.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Event published when stock reservation is rolled back
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRollbackEvent extends BaseEvent {
    private String reservationId;
    private String orderId;
    private List<RollbackItem> items;
    private String reason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackItem {
        private String productId;
        private Integer quantity;
    }
}
