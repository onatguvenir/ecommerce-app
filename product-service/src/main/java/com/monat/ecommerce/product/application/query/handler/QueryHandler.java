package com.monat.ecommerce.product.application.query.handler;

/**
 * Generic Query Handler arayüzü.
 * <p>
 * CQRS Read Side: Query handler'lar sistemi değiştirmez (side-effect free).
 * Bu sayede:
 * - Read model'i write model'den bağımsız optimize edilebilir
 * - Query handler'lar cache, circuit breaker gibi cross-cutting concern'lerle
 * kolayca dekore edilebilir
 * - Test edilmesi daha kolaydır (pure function'a yakın)
 * </p>
 *
 * @param <Q> Query tipi
 * @param <R> Dönüş tipi
 */
@FunctionalInterface
public interface QueryHandler<Q, R> {

    /**
     * Verilen query'yi işler ve sonucu döner.
     * Implementasyonlar side-effect içermemelidir.
     *
     * @param query sorgu nesnesi
     * @return sorgu sonucu
     */
    R handle(Q query);
}
