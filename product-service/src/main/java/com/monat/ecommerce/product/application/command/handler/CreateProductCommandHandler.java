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
 * CQRS Write Side: Sadece yazma operasyonundan sorumludur.
 * <p>
 * Sorumlulukları:
 * 1. Business rule doğrulaması (duplicate productId kontrolü)
 * 2. Domain nesnesi oluşturma
 * 3. MongoDB'ye persist etme
 * 4. Domain event yayınlama (Elasticsearch sync asenkron yapılır)
 * <p>
 * ApplicationEventPublisher kullanımı sayesinde Elasticsearch senkronizasyonu
 * write path'ten ayrılmış, asenkron hale getirilmiştir. Bu sayede:
 * - Write latency düşer (ES yavaşlasa bile MongoDB yazma etkilenmez)
 * - Loose coupling sağlanır
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
     * @Transactional: MongoDB yazma işlemini transaction içine alır.
     *                 Event publish, transaction commit'ten SONRA gerçekleşir
     *                 (TransactionalEventListener ile kullanılabilir).
     */
    @Override
    @Transactional
    public ProductResponse handle(CreateProductCommand command) {
        log.info("Handling CreateProductCommand for productId: {}", command.productId());

        // Guard clause: business key uniqueness kontrolü
        if (productRepository.existsByProductId(command.productId())) {
            throw new IllegalArgumentException("Product ID already exists: " + command.productId());
        }

        // Domain nesnesi oluşturma — MapStruct kullanılıyor
        Product product = productMapper.toEntity(command);
        product = productRepository.save(product);

        log.info("Product persisted to MongoDB: {}", product.getProductId());

        // Domain event yayınla
        eventPublisher.publishEvent(new ProductCreatedEvent(this, product));

        return productMapper.toResponse(product);
    }
}
