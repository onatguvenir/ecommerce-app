package com.monat.ecommerce.inventory.domain.repository;

import com.monat.ecommerce.inventory.domain.model.Inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

    Inventory save(Inventory inventory);

    Optional<Inventory> findById(UUID id);

    /**
     * Find inventory by product ID
     */
    Optional<Inventory> findByProductId(String productId);

    /**
     * Find products with low stock (for alerts)
     */
    List<Inventory> findLowStockProducts(Integer threshold);

    /**
     * Find products by IDs
     */
    List<Inventory> findByProductIdIn(List<String> productIds);

    /**
     * Check if product has sufficient stock
     */
    boolean hasAvailableStock(String productId, Integer quantity);

    long count();

    void deleteAll();
}
