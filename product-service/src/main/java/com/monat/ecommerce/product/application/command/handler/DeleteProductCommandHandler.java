package com.monat.ecommerce.product.application.command.handler;

import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.product.application.command.DeleteProductCommand;
import com.monat.ecommerce.product.domain.event.ProductDeletedEvent;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DeleteProductCommand Handler.
 * <p>
 * Write Side: Handles the deletion operation.
 * <p>
 * @CacheEvict: Evicts the deleted product from the Redis cache.
 *              allEntries=false: Only the specific entry is removed; other entries are preserved.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteProductCommandHandler implements CommandHandler<DeleteProductCommand, Void> {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Void handle(DeleteProductCommand command) {
        log.info("Handling DeleteProductCommand for productId: {}", command.productId());

        Product product = productRepository.findByProductId(command.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + command.productId()));

        productRepository.delete(product);

        log.info("Product deleted from MongoDB: {}", command.productId());

        // Remove from Elasticsearch asynchronously
        eventPublisher.publishEvent(new ProductDeletedEvent(this, product.getId(), command.productId()));

        return null;
    }
}
