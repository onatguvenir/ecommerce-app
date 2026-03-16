package com.monat.ecommerce.product.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Task Executor Konfigürasyonu.
 * <p>
 * 
 * @EnableAsync: @Async anotasyonlu metodların ayrı thread'lerde çalışmasını
 *               sağlar.
 *               <p>
 *               Neden özel ThreadPool?
 *               - Spring'in default SimpleAsyncTaskExecutor her çağrıda yeni
 *               thread açar (unbounded).
 *               - ThreadPoolTaskExecutor: bounded pool, backpressure ve
 *               monitoring desteği.
 *               - Elasticsearch indexleme gibi I/O-bound işlemler için optimize
 *               edilmiş ayarlar.
 *               <p>
 *               Thread Pool Boyutlandırma:
 *               - corePoolSize: Her zaman hazır bekleyen thread sayısı
 *               - maxPoolSize: Yoğun dönemde açılabilecek maksimum thread
 *               - queueCapacity: Thread'ler dolduğunda bekleyecek task sayısı
 *               </p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * "productSyncExecutor" adlı thread pool.
     * ProductSyncService'deki @Async metodlar bu pool'u kullanır.
     */
    @Bean(name = "productSyncExecutor")
    public Executor productSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // I/O-bound ES operasyonları için: CPU core * 2 + 1 formülü
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("es-sync-");
        // Shutdown sırasında bekleyen task'ların tamamlanmasını bekle
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
