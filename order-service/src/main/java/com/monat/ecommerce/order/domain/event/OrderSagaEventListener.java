package com.monat.ecommerce.order.domain.event;

import com.monat.ecommerce.order.domain.service.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderSagaEventListener {

    private final OrderSagaOrchestrator sagaOrchestrator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderSagaStarted(OrderSagaStartedEvent event) {
        sagaOrchestrator.executeOrderSaga(event.orderId(), event.cartId());
    }
}
