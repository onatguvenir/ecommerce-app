package com.monat.ecommerce.inventory.infrastructure.persistence.adapter;

import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import com.monat.ecommerce.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.monat.ecommerce.inventory.infrastructure.persistence.mapper.InventoryEntityMapper;
import com.monat.ecommerce.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository jpaRepository;
    private final InventoryEntityMapper mapper;

    @Override
    public Inventory save(Inventory inventory) {
        InventoryEntity entity = mapper.toEntity(inventory);
        InventoryEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Inventory> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Inventory> findByProductId(String productId) {
        return jpaRepository.findByProductId(productId).map(mapper::toDomain);
    }

    @Override
    public List<Inventory> findLowStockProducts(Integer threshold) {
        return jpaRepository.findLowStockProducts(threshold)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Inventory> findByProductIdIn(List<String> productIds) {
        return jpaRepository.findByProductIdIn(productIds)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasAvailableStock(String productId, Integer quantity) {
        return jpaRepository.hasAvailableStock(productId, quantity);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
