package com.monat.ecommerce.notification.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * Asenkron görev konfigürasyonu.
 *
 * JDK 21'de Virtual Thread'ler (Project Loom) ile Spring'in @Async
 * entegrasyonu yapılır. Bu sayede e-posta ve SMS gönderimleri gibi
 * I/O bağımlı işlemler platform thread'lerini bloke etmeden çalışır.
 *
 * Virtual Thread: Her göreve anında ayrılan, hafif (heap-based) iş parçacığı.
 * Platform Thread poolları yerine JVM tarafından yönetilir. JDK 21'den itibaren
 * stabil olarak kullanılabilir.
 *
 * spring.threads.virtual.enabled=true ile Spring Boot 3.2+,
 * Tomcat, Scheduled ve @Async işlemlerini otomatik olarak virtual
 * thread üzerinde çalıştırır. Bu bean ek güvence sağlar.
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * Virtual Thread tabanlı Executor.
     *
     * Thread.ofVirtual().factory() ile oluşturulan her sanal thread:
     *   - Ayrı bir stack'e sahip değil (heap üzerinde)
     *   - Bloke olduğunda otomatik park edilir
     *   - JVM scheduler tarafından carrier thread havuzuna dağıtılır
     *
     * "taskExecutor" ismi Spring'in @Async için kullandığı default executor adıdır.
     */
    @Bean("taskExecutor")
    public Executor virtualThreadExecutor() {
        log.info("Configuring Virtual Thread Executor for @Async methods");
        // Her görev için yeni bir virtual thread oluşturur — pool overhead yok
        return command -> Thread.ofVirtual()
                .name("async-vt-", 0)
                .start(command);
    }
}
