package com.monat.ecommerce.notification.infrastructure.messaging;

import com.monat.ecommerce.events.order.OrderCancelledEvent;
import com.monat.ecommerce.events.order.OrderCompletedEvent;
import com.monat.ecommerce.events.order.OrderCreatedEvent;
import com.monat.ecommerce.grpc.user.User;
import com.monat.ecommerce.notification.domain.service.EmailService;
import com.monat.ecommerce.notification.domain.service.SmsService;
import com.monat.ecommerce.notification.infrastructure.config.NotificationMetrics;
import com.monat.ecommerce.notification.infrastructure.grpc.UserServiceClient;
import com.monat.ecommerce.notification.infrastructure.persistence.ProcessedEventRepository;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    /**
     * NotificationMetrics must be declared as @Mock so Mockito's @InjectMocks
     * can inject it into OrderEventConsumer via constructor injection.
     * Without this declaration the field remains null, causing NPE when
     * notificationMetrics.consumerTimer() is called in the finally block.
     */
    @Mock
    private NotificationMetrics notificationMetrics;

    /**
     * Timer.Sample returned by Timer.start() – mocked so sample.stop() won't
     * throw when it receives the mocked Timer from notificationMetrics.consumerTimer().
     */
    @Mock
    private Timer mockTimer;

    private String orderId;
    private String userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();

        testUser = User.newBuilder()
                .setId(userId)
                .setEmail("test@exampl.com")
                .setFirstName("John")
                .setLastName("Doe")
                .build();

        // Wire notificationMetrics.consumerTimer() → a mock Timer so that
        // sample.stop(timer) has a non-null argument and doesn't NPE.
        when(notificationMetrics.consumerTimer()).thenReturn(mockTimer);
    }

    @Test
    void handleOrderCreated_ShouldProcessAndSendEmail() {
        // Arrange
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .orderNumber("ORD-123")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        when(processedEventRepository.existsByEventIdAndEventType(orderId, "ORDER_CREATED"))
                .thenReturn(false);
        when(userServiceClient.getUser(userId)).thenReturn(Optional.of(testUser));

        // Act
        orderEventConsumer.handleOrderCreated(event);

        // Assert
        verify(emailService, times(1)).sendOrderConfirmation(
                eq("test@exampl.com"),
                eq("ORD-123"),
                eq("John Doe"),
                eq("100.00")
        );
        verify(processedEventRepository, times(1)).save(any());
    }

    @Test
    void handleOrderCreated_ShouldSkipIfAlreadyProcessed() {
        // Arrange
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .build();

        when(processedEventRepository.existsByEventIdAndEventType(orderId, "ORDER_CREATED"))
                .thenReturn(true);

        // Act
        orderEventConsumer.handleOrderCreated(event);

        // Assert
        verify(userServiceClient, never()).getUser(anyString());
        verify(emailService, never()).sendOrderConfirmation(anyString(), anyString(), anyString(), anyString());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handleOrderCompleted_ShouldProcessAndSendEmail() {
        // Arrange
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .orderNumber("ORD-123")
                .build();

        when(processedEventRepository.existsByEventIdAndEventType(orderId, "ORDER_COMPLETED"))
                .thenReturn(false);
        when(userServiceClient.getUser(userId)).thenReturn(Optional.of(testUser));

        // Act
        orderEventConsumer.handleOrderCompleted(event);

        // Assert
        verify(emailService, times(1)).sendOrderCompleted(
                eq("test@exampl.com"),
                eq("ORD-123"),
                eq("John Doe")
        );
        verify(processedEventRepository, times(1)).save(any());
    }

    @Test
    void handleOrderCancelled_ShouldProcessAndSendEmail() {
        // Arrange
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .orderNumber("ORD-123")
                .reason("Out of stock")
                .build();

        when(processedEventRepository.existsByEventIdAndEventType(orderId, "ORDER_CANCELLED"))
                .thenReturn(false);
        when(userServiceClient.getUser(userId)).thenReturn(Optional.of(testUser));

        // Act
        orderEventConsumer.handleOrderCancelled(event);

        // Assert
        verify(emailService, times(1)).sendOrderCancelled(
                eq("test@exampl.com"),
                eq("ORD-123"),
                eq("John Doe"),
                eq("Out of stock")
        );
        verify(processedEventRepository, times(1)).save(any());
    }
}
