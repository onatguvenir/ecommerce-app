package com.monat.ecommerce.product.infrastructure.persistence.repository;

import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.infrastructure.persistence.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product MongoDB Repository.
 * <p>
 * This interface provides data access operations for the ProductDocument in
 * MongoDB.
 * </p>
 * 
 * @Repository is a Spring annotation that indicates that the decorated class is
 *             a repository.
 * 
 *             Extends MongoRepository<ProductDocument, String>:
 *             - Inherits standard CRUD operations specific to MongoDB.
 *             - The ID type is String because MongoDB ObjectIds are typically
 *             represented as Strings in Java.
 */
@Repository
public interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {

    Optional<ProductDocument> findByProductId(String productId);

    Page<ProductDocument> findByStatus(ProductStatus status, Pageable pageable);

    Page<ProductDocument> findByCategory(String category, Pageable pageable);

    Page<ProductDocument> findByCategoryAndStatus(String category, ProductStatus status, Pageable pageable);

    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    Page<ProductDocument> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("{ 'price': { $gte: ?0, $lte: ?1 } }")
    Page<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    List<ProductDocument> findByBrand(String brand);

    @Query("{ 'tags': ?0 }")
    List<ProductDocument> findByTag(String tag);

    boolean existsByProductId(String productId);
}
