package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.events.OrderShippedEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// ORDER.MARK_SHIPPED (Seller only). No real carrier/tracking -- this is just the Seller
// manually declaring "I shipped this", which RefundOrderUseCase and ReturnRequest use to
// decide how to handle the goods side of a later refund.
public class MarkOrderShippedUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public MarkOrderShippedUseCase(OrderRepositoryPort orderRepositoryPort, EventPublisherPort eventPublisherPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public OrderResult markShipped(String orderId, String sellerId) {
        Order order = orderRepositoryPort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        boolean ownsAnItem = order.getItems().stream().anyMatch(item -> item.getSellerId().equals(sellerId));
        if (!ownsAnItem) {
            throw new ForbiddenException("NOT_THE_SELLER",
                    "Only a seller with items in this order can mark it shipped");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ConflictException("ORDER_NOT_SHIPPABLE", "Order " + orderId + " is not in a shippable state");
        }
        if (order.getShippedAt() != null) {
            throw new ConflictException("ORDER_ALREADY_SHIPPED",
                    "Order " + orderId + " has already been marked shipped");
        }

        Order saved = orderRepositoryPort.save(order.markShipped(Instant.now()));
        // user-service records (order, buyer, seller) from this event; a rating is only accepted
        // between the two parties of a shipped order. An order has ONE shippedAt, so the seller who
        // pressed "shipped" is the one recorded.
        eventPublisherPort.publish(new OrderShippedEvent(saved.getId(), saved.getAuctionId(), saved.getBuyerId(),
                sellerId, saved.getShippedAt()));
        return OrderResult.from(saved);
    }
}
