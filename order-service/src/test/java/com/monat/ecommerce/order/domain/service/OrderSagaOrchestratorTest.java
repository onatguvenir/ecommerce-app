package com.monat.ecommerce.order.domain.service;

import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.domain.repository.OrderSagaStateRepository;
import com.monat.ecommerce.order.domain.repository.OutboxEventRepository;
import com.monat.ecommerce.order.infrastructure.client.CartClient;
import com.monat.ecommerce.order.infrastructure.config.OrderMetrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the cart deletion step of the order saga.
 *
 * <p>Cart deletion runs at the tail of a successful order and must be:
 * idempotent, retried on transient failure, and never silently swallowed
 * (a final failure is recorded as a metric and logged at ERROR).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSagaOrchestrator — cart deletion")
class OrderSagaOrchestratorTest {

    @Mock
    private CartClient cartClient;

    @Mock
    private OrderMetrics orderMetrics;

    private OrderSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new OrderSagaOrchestrator(
                mock(OrderRepository.class),
                mock(OrderSagaStateRepository.class),
                mock(OutboxEventRepository.class),
                new ObjectMapper(),
                orderMetrics,
                cartClient,
                ObservationRegistry.NOOP);
    }

    @Test
    @DisplayName("deletes cart on first success")
    void deletesCartOnFirstSuccess() {
        orchestrator.deleteCartAfterCompletion("cart-1");

        verify(cartClient, times(1)).deleteCart("cart-1");
        verify(orderMetrics).incrementSagaStep("delete_cart", "success");
    }

    @Test
    @DisplayName("retries after a transient failure then succeeds")
    void retriesThenSucceeds() {
        when(cartClient.deleteCart("cart-1"))
                .thenThrow(new RuntimeException("transient cart-service blip"))
                .thenReturn(null);

        orchestrator.deleteCartAfterCompletion("cart-1");

        verify(cartClient, times(2)).deleteCart("cart-1");
        verify(orderMetrics).incrementSagaStep("delete_cart", "retry");
        verify(orderMetrics).incrementSagaStep("delete_cart", "success");
    }

    @Test
    @DisplayName("exhausts retries, marks failed, does not throw")
    void retriesExhaustedMarksFailed() {
        when(cartClient.deleteCart("cart-1")).thenThrow(new RuntimeException("cart-service down"));

        orchestrator.deleteCartAfterCompletion("cart-1"); // must not propagate

        verify(cartClient, times(3)).deleteCart("cart-1");
        verify(orderMetrics).incrementSagaStep("delete_cart", "failed");
    }

    @Test
    @DisplayName("skips deletion when cartId is null or blank")
    void skipsWhenCartIdMissing() {
        orchestrator.deleteCartAfterCompletion(null);
        orchestrator.deleteCartAfterCompletion("   ");

        verifyNoInteractions(cartClient);
    }
}
