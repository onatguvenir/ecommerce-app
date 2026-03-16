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
 * Write Side: Güncelleme operasyonu.
 * <p>
 * 
 * @CacheEvict: Güncelleme sonrası Redis cache'deki eski veriyi temizler.
 *              Cache key olarak productId kullanılır — bu sayede sonraki GET
 *              isteği
 *              taze veriyi MongoDB'den çeker ve cache'e yazar.
 *              </p>
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

        // Mevcut ürünü bul
        Product product = productRepository.findByProductId(command.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + command.productId()));

        // Domain nesnesini güncelle — MapStruct kullanılıyor
        productMapper.updateEntityFromCommand(command, product);
        product = productRepository.save(product);

        log.info("Product updated in MongoDB: {}", product.getProductId());

        // Elasticsearch'ü asenkron güncelle
        eventPublisher.publishEvent(new ProductUpdatedEvent(this, product));

        return productMapper.toResponse(product);
    }
}
