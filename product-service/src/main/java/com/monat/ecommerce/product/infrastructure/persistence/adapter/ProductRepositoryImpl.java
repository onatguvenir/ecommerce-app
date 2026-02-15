package com.monat.ecommerce.product.infrastructure.persistence.adapter;

import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.model.ProductStatus;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import com.monat.ecommerce.product.infrastructure.persistence.document.ProductDocument;
import com.monat.ecommerce.product.infrastructure.persistence.mapper.ProductMapper;
import com.monat.ecommerce.product.infrastructure.persistence.repository.ProductMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductMongoRepository mongoRepository;
    private final ProductMapper mapper;

    @Override
    public Product save(Product product) {
        ProductDocument document = mapper.toDocument(product);
        ProductDocument savedDocument = mongoRepository.save(document);
        return mapper.toDomain(savedDocument);
    }

    @Override
    public List<Product> saveAll(List<Product> products) {
        List<ProductDocument> documents = products.stream()
                .map(mapper::toDocument)
                .collect(Collectors.toList());
        List<ProductDocument> savedDocuments = mongoRepository.saveAll(documents);
        return savedDocuments.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Product> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findByProductId(String productId) {
        return mongoRepository.findByProductId(productId).map(mapper::toDomain);
    }

    @Override
    public Page<Product> findByStatus(ProductStatus status, Pageable pageable) {
        return mongoRepository.findByStatus(status, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Product> findByCategory(String category, Pageable pageable) {
        return mongoRepository.findByCategory(category, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Product> findByCategoryAndStatus(String category, ProductStatus status, Pageable pageable) {
        return mongoRepository.findByCategoryAndStatus(category, status, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable) {
        return mongoRepository.findByNameContainingIgnoreCase(name, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return mongoRepository.findByPriceBetween(minPrice, maxPrice, pageable).map(mapper::toDomain);
    }

    @Override
    public List<Product> findByBrand(String brand) {
        return mongoRepository.findByBrand(brand)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByTag(String tag) {
        return mongoRepository.findByTag(tag)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByProductId(String productId) {
        return mongoRepository.existsByProductId(productId);
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return mongoRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void delete(Product product) {
        if (product.getId() != null) {
            mongoRepository.deleteById(product.getId());
        } else if (product.getProductId() != null) {
            // Fallback if ID is missing but business key is present, though unlikely for
            // deletion by entity
            // Better to just delete by id if present.
            // If product is a domain object without ID but with business key, we might need
            // to find first.
            // But usually domain object from repository has ID.
        }
    }

    @Override
    public void deleteById(String id) {
        mongoRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        mongoRepository.deleteAll();
    }

    @Override
    public long count() {
        return mongoRepository.count();
    }
}
