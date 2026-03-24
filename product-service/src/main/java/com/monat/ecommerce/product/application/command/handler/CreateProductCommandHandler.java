package com.monat.ecommerce.product.application.command.handler;

import com.monat.ecommerce.product.application.command.CreateProductCommand;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.mapper.ProductMapper;
import com.monat.ecommerce.product.domain.event.ProductCreatedEvent;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

/**
 * CreateProductCommand Handler.
 * <p>
 * CQRS Write Side: Responsible solely for write operations.
 * <p>
 * Responsibilities:
 * 1. Business rule validation (e.g., checking for duplicate productId).
 * 2. Domain object creation.
 * 3. Persistence to MongoDB.
 * 4. Domain event publication (Elasticsearch synchronization is handled asynchronously).
 * <p>
 * Using ApplicationEventPublisher decouples Elasticsearch synchronization from the write path, 
 * making it asynchronous. This results in:
 * - Reduced write latency (MongoDB writes are unaffected by ES performance).
 * - Loose coupling between components.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateProductCommandHandler implements CommandHandler<CreateProductCommand, ProductResponse> {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductMapper productMapper;

    /**
     * @Transactional: Wraps the MongoDB write operation in a transaction.
     *                 Event publication occurs AFTER the transaction is committed
     *                 (can be further refined with TransactionalEventListener).
     */
    @Override
    @Transactional
    public ProductResponse handle(CreateProductCommand command) {
        log.info("Handling CreateProductCommand for productId: {}", command.productId());

        // Guard clause: enforce business key uniqueness
        if (productRepository.existsByProductId(command.productId())) {
            throw new IllegalArgumentException("Product ID already exists: " + command.productId());
        }

        // Domain object mapping via MapStruct
        Product product = productMapper.toEntity(command);
        product = productRepository.save(product);

        log.info("Product persisted to MongoDB: {}", product.getProductId());

        // Publish domain event
        eventPublisher.publishEvent(new ProductCreatedEvent(this, product));

        return productMapper.toResponse(product);
    }
}
