package com.monat.ecommerce.product.domain.repository;

import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    List<Product> saveAll(List<Product> products);

    Optional<Product> findById(String id);

    Optional<Product> findByProductId(String productId);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByCategoryAndStatus(String category, ProductStatus status, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    List<Product> findByBrand(String brand);

    List<Product> findByTag(String tag);

    boolean existsByProductId(String productId);

    Page<Product> findAll(Pageable pageable);

    void delete(Product product);

    void deleteById(String id);

    void deleteAll();

    long count();
}
