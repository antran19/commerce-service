package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.events.OrderCancelledEvent;
import org.springframework.transaction.annotation.Transactional;

// System-triggered cancel (no caller/ownership check, unlike CancelOrderUseCase): triggered
// by fulfillment-service's InventoryReservationFailed, meaning stock couldn't be held for
// this order. Publishes the same OrderCancelledEvent as a buyer-initiated cancel so
// auction-service's existing handling (see its CommerceEventsListener) stays unified
// regardless of why the order was cancelled.
public class CancelOrderDueToStockFailureUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public CancelOrderDueToStockFailureUseCase(OrderRepositoryPort orderRepositoryPort,
                                                EventPublisherPort eventPublisherPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public void cancel(String orderId, String reason) {
        Order order = orderRepositoryPort.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            return;
        }

        Order saved = orderRepositoryPort.save(order.cancel());
        eventPublisherPort.publish(new OrderCancelledEvent(saved.getId(), reason, saved.getAuctionId()));
    }
}
