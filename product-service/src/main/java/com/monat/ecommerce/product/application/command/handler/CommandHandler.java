package com.monat.ecommerce.product.application.command.handler;

/**
 * Generic Command Handler arayüzü.
 * <p>
 * Strategy Pattern: Her command tipi için ayrı bir handler implementasyonu
 * bulunur. Bu sayede Open/Closed Principle sağlanır — yeni bir command
 * eklemek mevcut kodu değiştirmez, sadece yeni bir handler eklenir.
 * </p>
 *
 * @param <C> Command tipi
 * @param <R> Dönüş tipi (void işlemler için Void kullanılır)
 */
@FunctionalInterface
public interface CommandHandler<C, R> {

    /**
     * Verilen command'ı işler ve sonucu döner.
     *
     * @param command işlenecek command nesnesi
     * @return işlem sonucu
     */
    R handle(C command);
}
