package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.events.OrderCancelledEvent;
import org.springframework.transaction.annotation.Transactional;

public class CancelOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public CancelOrderUseCase(OrderRepositoryPort orderRepositoryPort, EventPublisherPort eventPublisherPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    // v1 scope: only the owning buyer can cancel, and only before payment -- cancelling a paid
    // order would require a real refund flow, which is deliberately out of scope for this pass.
    // @Transactional is required here: findByIdForUpdate takes a PESSIMISTIC_WRITE lock, which
    // Hibernate refuses to acquire outside an active transaction.
    @Transactional
    public OrderResult cancel(String orderId, String callerId) {
        Order order = orderRepositoryPort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getBuyerId().equals(callerId)) {
            throw new ForbiddenException("NOT_THE_BUYER", "Only the order's buyer can cancel it");
        }
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new ConflictException("ORDER_NOT_CANCELLABLE",
                    "Order " + orderId + " can no longer be cancelled");
        }

        Order saved = orderRepositoryPort.save(order.cancel());
        eventPublisherPort.publish(new OrderCancelledEvent(saved.getId(), "BUYER_REQUESTED", saved.getAuctionId()));
        return OrderResult.from(saved);
    }
}
