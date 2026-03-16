package com.monat.ecommerce.order.infrastructure.bootstrap;

import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderItem;
import com.monat.ecommerce.order.domain.model.OrderStatus;
import com.monat.ecommerce.order.domain.model.ShippingAddress;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seeds the database with dummy orders.
 * Only runs when 'docker' profile is active and database is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class OrderDataSeeder implements CommandLineRunner {

        private final OrderRepository orderRepository;

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                if (orderRepository.count() > 0) {
                        log.info("Orders already exist. Skipping seeding.");
                        return;
                }

                log.info("Seeding orders...");

                // Note: In a real scenario, we would fetch User IDs from User Service.
                // For simplicity in this dummy data seeder, we will generate a random User ID
                // or assume the one created by UserDataSeeder if we could share state (which we
                // can't easily across services).
                // Here we'll use a fixed UUID that we hope matches, or just a random one since
                // we aren't enforcing FKs across microservices.
                UUID userId = UUID.randomUUID(); // Placeholder user ID

                // Order 1: Completed
                createOrder(userId, "ORD-2023-001", OrderStatus.COMPLETED, new BigDecimal("1199.98"),
                                "PROD-001", "Smartphone X Pro", new BigDecimal("999.99"), 1,
                                "PROD-003", "Wireless Earbuds", new BigDecimal("199.99"), 1);

                // Order 2: Processing
                createOrder(userId, "ORD-2023-002", OrderStatus.CONFIRMED, new BigDecimal("149.99"),
                                "PROD-011", "Classic T-Shirt", new BigDecimal("24.99"), 2,
                                "PROD-012", "Denim Jeans", new BigDecimal("100.00"), 1);

                // Order 3: Cancelled
                createOrder(userId, "ORD-2023-003", OrderStatus.CANCELLED, new BigDecimal("449.99"),
                                "PROD-005", "4K Monitor 27\"", new BigDecimal("449.99"), 1,
                                null, null, null, 0);

                log.info("Seeding orders completed. Created {} orders.", orderRepository.count());
        }

        private void createOrder(UUID userId, String orderNumber, OrderStatus status, BigDecimal total,
                        String p1Id, String p1Name, BigDecimal p1Price, int p1Qty,
                        String p2Id, String p2Name, BigDecimal p2Price, int p2Qty) {

                Order order = Order.builder()
                                .orderNumber(orderNumber)
                                .userId(userId)
                                .status(status)
                                .totalAmount(total)
                                .currency("USD")
                                .createdAt(LocalDateTime.now().minusDays(1))
                                .updatedAt(LocalDateTime.now())
                                .shippingAddress(ShippingAddress.builder()
                                                // .fullName("John Doe") // Removed as it doesn't exist in
                                                // ShippingAddress
                                                .street("123 Main St")
                                                .city("New York")
                                                .state("NY")
                                                .country("USA")
                                                .postalCode("10001")
                                                .build())
                                .build();

                OrderItem item1 = OrderItem.builder()
                                .productId(p1Id)
                                .productName(p1Name)
                                .quantity(p1Qty)
                                .unitPrice(p1Price)
                                .subtotal(p1Price.multiply(BigDecimal.valueOf(p1Qty)))
                                .build();

                order.addItem(item1);

                if (p2Id != null && p2Price != null) {
                        OrderItem item2 = OrderItem.builder()
                                        .productId(p2Id)
                                        .productName(p2Name)
                                        .quantity(p2Qty)
                                        .unitPrice(p2Price)
                                        .subtotal(p2Price.multiply(BigDecimal.valueOf(p2Qty)))
                                        .build();
                        order.addItem(item2);
                }

                orderRepository.save(order);
        }
}
