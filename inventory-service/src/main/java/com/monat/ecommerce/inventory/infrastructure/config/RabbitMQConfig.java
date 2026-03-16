package com.monat.ecommerce.inventory.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Inventory Bulk Update.
 *
 * Spring Batch tarafından okunan kayıtlar (chunk) doğrudan veritabanına
 * yazılmak yerine bu konfigürasyon üzerinden RabbitMQ'ya kuyruğa (queue) bırakılır.
 * Bu sayede Consumer tarafı kendi hızında DB transaction'larını ve Pessimistic
 * Locking işlemlerini bloklamadan (throttle) yürütebilir.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "inventory.exchange";
    
    // Ana işlem kuyruğu
    public static final String STOCK_UPDATE_QUEUE = "inventory.stock.update.queue";
    public static final String STOCK_UPDATE_ROUTING_KEY = "inventory.stock.update.routingKey";
    
    // Dead Letter Queue (DLQ) — Hata alan mesajların saklanacağı yer
    public static final String STOCK_UPDATE_DLQ = "inventory.stock.update.dlq";
    public static final String STOCK_UPDATE_DLQ_ROUTING_KEY = "inventory.stock.update.dlq.routingKey";

    /**
     * Mesajları sınıflara çevirmek için Jackson 2 JSON parser kullanılır.
     * Record tiplerini de destekler.
     */
    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate için JSON çeviriciyi aktif etme.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
        return rabbitTemplate;
    }

    // --- Exchange Definition ---

    @Bean
    public DirectExchange inventoryExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    // --- Queue Definitions ---

    /**
     * Ana işlem kuyruğu. Herhangi bir hatada DLQ'ya yönlendirilmesi için argümanlar belirleriz.
     */
    @Bean
    public Queue stockUpdateQueue() {
        return QueueBuilder.durable(STOCK_UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", STOCK_UPDATE_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Dead Letter Queue (DLQ). Tüm retry'lara rağmen işlenemeyen hatalı kayıtlar buraya düşer.
     */
    @Bean
    public Queue stockUpdateDlq() {
        return QueueBuilder.durable(STOCK_UPDATE_DLQ).build();
    }

    // --- Bindings ---

    @Bean
    public Binding bindingStockUpdateQueue(Queue stockUpdateQueue, DirectExchange inventoryExchange) {
        return BindingBuilder.bind(stockUpdateQueue)
                .to(inventoryExchange)
                .with(STOCK_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingStockUpdateDlq(Queue stockUpdateDlq, DirectExchange inventoryExchange) {
        return BindingBuilder.bind(stockUpdateDlq)
                .to(inventoryExchange)
                .with(STOCK_UPDATE_DLQ_ROUTING_KEY);
    }
}
