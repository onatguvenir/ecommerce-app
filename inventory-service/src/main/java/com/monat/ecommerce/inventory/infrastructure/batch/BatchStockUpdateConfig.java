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
 * Spring Batch Configuration for managing bulk stock updates.
 * 
 * To avoid database write contention at the I/O boundary, this configuration 
 * uses an AmqpItemWriter (RabbitMQ) instead of a direct database writer (JpaItemWriter).
 * The actual database updates are handled asynchronously by RabbitMQ consumers.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchStockUpdateConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Step 1: Reads data from a CSV file.
     * @param filePath Receives the file path as a JobParameter.
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
                .linesToSkip(1) // Skip header row
                .fieldSetMapper(fieldSet -> new StockUpdateMessage(
                        fieldSet.readString("sku"),
                        fieldSet.readInt("quantity"),
                        fieldSet.readString("operationType"),
                        fieldSet.readString("referenceId")
                ))
                .build();
    }

    /**
     * Step 2: Validates the read data (Validation/Filtering).
     */
    @Bean
    public ItemProcessor<StockUpdateMessage, StockUpdateMessage> stockProcessor() {
        return item -> {
            if (item.quantity() < 0) {
                log.warn("Negative stock quantity skipped. SKU: {}", item.sku());
                return null; // Filters (skip)
            }
            if (!"ADD".equalsIgnoreCase(item.operationType()) && !"SET".equalsIgnoreCase(item.operationType())) {
                log.warn("Invalid operation type skipped. SKU: {}, Opr: {}", item.sku(), item.operationType());
                return null; // Filters
            }
            return item;
        };
    }

    /**
     * Step 3: Sends processed data to RabbitMQ (AmqpItemWriter).
     * Pushed to the queue when the chunk size (e.g., 100) is reached.
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
     * Step Definition: chunkSize(100) -> Flushes to the queue every 100 rows.
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
     * Job Definition.
     */
    @Bean
    public Job bulkStockUpdateJob() {
        return new JobBuilder("bulkStockUpdateJob", jobRepository)
                .start(bulkStockUpdateStep())
                .build();
    }
}
