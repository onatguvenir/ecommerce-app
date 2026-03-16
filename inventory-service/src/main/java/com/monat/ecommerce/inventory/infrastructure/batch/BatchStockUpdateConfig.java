package com.monat.ecommerce.inventory.infrastructure.batch;

import com.monat.ecommerce.inventory.domain.dto.StockUpdateMessage;
import com.monat.ecommerce.inventory.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.amqp.AmqpItemWriter;
import org.springframework.batch.item.amqp.builder.AmqpItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Stok toplu güncellemelerini yöneten Spring Batch Configuration.
 * 
 * Veritabanını I/O sınırında kitlememek (lock contention) için ItemWriter olarak
 * doğrudan veritabanı (JpaItemWriter) yerine RabbitMQ'ya (AmqpItemWriter)
 * yazar. Asıl veritabanı update işlemini RabbitMQ Consumer'lar gerçekleştirir.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchStockUpdateConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Adım 1: CSV dosyasından veriyi okur.
     * @param filePath JobParametresi olarak dosya yolunu alır.
     */
    @Bean
    @StepScope
    public FlatFileItemReader<StockUpdateMessage> csvStockReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        
        return new FlatFileItemReaderBuilder<StockUpdateMessage>()
                .name("csvStockReader")
                .resource(new FileSystemResource(filePath))
                .delimited()
                .names("sku", "quantity", "operationType", "referenceId")
                .linesToSkip(1) // Header satırını atla
                .fieldSetMapper(fieldSet -> new StockUpdateMessage(
                        fieldSet.readString("sku"),
                        fieldSet.readInt("quantity"),
                        fieldSet.readString("operationType"),
                        fieldSet.readString("referenceId")
                ))
                .build();
    }

    /**
     * Adım 2: Formattan geçen veriyi doğrular (Validation/Filtering).
     */
    @Bean
    public ItemProcessor<StockUpdateMessage, StockUpdateMessage> stockProcessor() {
        return item -> {
            if (item.quantity() < 0) {
                log.warn("Negatif stok miktarı atlanıyor. SKU: {}", item.sku());
                return null; // Filtreler (atla)
            }
            if (!"ADD".equalsIgnoreCase(item.operationType()) && !"SET".equalsIgnoreCase(item.operationType())) {
                log.warn("Geçersiz operasyon tipi atlanıyor. SKU: {}, Opr: {}", item.sku(), item.operationType());
                return null; // Filtreler
            }
            return item;
        };
    }

    /**
     * Adım 3: İşlenmiş veriyi RabbitMQ'ya (AmqpItemWriter) yollar.
     * Chunck size (ör: 100) kadar biriktiğinde kuyruğa push edilir.
     */
    @Bean
    public ItemWriter<StockUpdateMessage> amqpStockWriter() {
        rabbitTemplate.setExchange(RabbitMQConfig.EXCHANGE_NAME);
        rabbitTemplate.setRoutingKey(RabbitMQConfig.STOCK_UPDATE_ROUTING_KEY);
        
        return new AmqpItemWriterBuilder<StockUpdateMessage>()
                .amqpTemplate(rabbitTemplate)
                .build();
    }

    /**
     * Step Tanımlaması: chunkSize(100) -> Her 100 satırda bir kuyruğa fırlatır.
     */
    @Bean
    public Step bulkStockUpdateStep() {
        return new StepBuilder("bulkStockUpdateStep", jobRepository)
                .<StockUpdateMessage, StockUpdateMessage>chunk(100, transactionManager)
                .reader(csvStockReader(null))
                .processor(stockProcessor())
                .writer(amqpStockWriter())
                .build();
    }

    /**
     * Job Tanımlaması.
     */
    @Bean
    public Job bulkStockUpdateJob() {
        return new JobBuilder("bulkStockUpdateJob", jobRepository)
                .start(bulkStockUpdateStep())
                .build();
    }
}
