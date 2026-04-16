package com.monat.ecommerce.product.application.query.handler;

import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.mapper.ProductMapper;
import com.monat.ecommerce.product.application.query.GetProductQuery;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * GetProductQuery Handler.
 * <p>
 * CQRS Read Side: Tekil ürün sorgusunu işler.
 * <p>
 * Redis Cache Stratejisi:
 * - @Cacheable("products"): İlk çağrıda MongoDB'den okur, Redis'e yazar.
 * - Sonraki çağrılarda Redis'ten döner (DB'ye gitmez).
 * - Cache key: productId (business key, daha okunabilir)
 * - TTL: application.yml'de spring.cache.redis.time-to-live ile yapılandırılır.
 * <p>
 * Cache Invalidation: UpdateProductCommandHandler ve
 * DeleteProductCommandHandler
 * 
 * @CacheEvict ile ilgili key'i temizler.
 *             </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetProductQueryHandler implements QueryHandler<GetProductQuery, ProductResponse> {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    /**
     * @Cacheable: Spring Cache abstraction üzerinden Redis'e erişir.
     *             condition: Sadece productId null değilse cache'e yaz.
     *             unless: Null sonuç cache'lenmez (ürün bulunamadı durumu).
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#p0.productId()", unless = "#result == null")
    public ProductResponse handle(GetProductQuery query) {
        log.debug("Cache miss — fetching product from MongoDB: {}", query.productId());

        Product product = productRepository.findByProductId(query.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + query.productId()));

        return productMapper.toResponse(product);
    }
}
