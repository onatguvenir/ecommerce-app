package com.monat.ecommerce.product.application.command.handler;

import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.product.application.command.UpdateProductCommand;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.mapper.ProductMapper;
import com.monat.ecommerce.product.domain.event.ProductUpdatedEvent;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

/**
 * UpdateProductCommand Handler.
 * <p>
 * Write Side: Handles the update operation.
 * <p>
 * @CacheEvict: Clears stale data from the Redis cache after an update.
 *              The productId is used as the cache key, ensuring subsequent GET 
 *              requests fetch fresh data from MongoDB and re-populate the cache.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateProductCommandHandler implements CommandHandler<UpdateProductCommand, ProductResponse> {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse handle(UpdateProductCommand command) {
        log.info("Handling UpdateProductCommand for productId: {}", command.productId());

        // Find existing product
        Product product = productRepository.findByProductId(command.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + command.productId()));

        // Update domain entity via MapStruct
        productMapper.updateEntityFromCommand(command, product);
        product = productRepository.save(product);

        log.info("Product updated in MongoDB: {}", product.getProductId());

        // Update Elasticsearch asynchronously
        eventPublisher.publishEvent(new ProductUpdatedEvent(this, product));

        return productMapper.toResponse(product);
    }
}
