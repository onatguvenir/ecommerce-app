package com.monat.ecommerce.product.domain.repository;

import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findByProductId(String productId);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByCategoryAndStatus(String category, ProductStatus status, Pageable pageable);

    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("{ 'price': { $gte: ?0, $lte: ?1 } }")
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    List<Product> findByBrand(String brand);

    @Query("{ 'tags': ?0 }")
    List<Product> findByTag(String tag);

    boolean existsByProductId(String productId);
}
