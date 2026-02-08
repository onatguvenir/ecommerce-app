package com.monat.ecommerce.inventory.domain.repository;

import com.monat.ecommerce.inventory.domain.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * Find inventory by product ID
     * Uses optimistic locking - no explicit lock needed
     */
    Optional<Inventory> findByProductId(String productId);

    /**
     * Find products with low stock (for alerts)
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity < :threshold")
    List<Inventory> findLowStockProducts(Integer threshold);

    /**
     * Find products by IDs
     */
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds")
    List<Inventory> findByProductIdIn(List<String> productIds);

    /**
     * Check if product has sufficient stock
     */
    @Query("SELECT CASE WHEN i.availableQuantity >= :quantity THEN true ELSE false END " +
           "FROM Inventory i WHERE i.productId = :productId")
    boolean hasAvailableStock(String productId, Integer quantity);
}
