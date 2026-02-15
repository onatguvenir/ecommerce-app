package com.monat.ecommerce.inventory.infrastructure.persistence.repository;

import com.monat.ecommerce.inventory.infrastructure.persistence.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, UUID> {

    Optional<InventoryEntity> findByProductId(String productId);

    @Query("SELECT i FROM InventoryEntity i WHERE i.availableQuantity < :threshold")
    List<InventoryEntity> findLowStockProducts(@Param("threshold") Integer threshold);

    @Query("SELECT i FROM InventoryEntity i WHERE i.productId IN :productIds")
    List<InventoryEntity> findByProductIdIn(@Param("productIds") List<String> productIds);

    @Query("SELECT CASE WHEN i.availableQuantity >= :quantity THEN true ELSE false END " +
            "FROM InventoryEntity i WHERE i.productId = :productId")
    boolean hasAvailableStock(@Param("productId") String productId, @Param("quantity") Integer quantity);
}
