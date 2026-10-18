package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.events.OrderCancelledEvent;
import org.springframework.transaction.annotation.Transactional;

// The auction's winner defaulted (AuctionPaymentTimeout: 24h without paying, or they cancelled
// the order themselves). Cancels their order if it is still waiting for payment, so it can no
// longer be paid after the unit went back on sale. Only touches an AWAITING_PAYMENT order:
// usually the winner's own cancel already did this, and a paid order must never be cancelled.
// auction-service sees the resulting OrderCancelled (reason PAYMENT_TIMEOUT) and treats it as
// already handled: the timeout that triggered this has set its flag.
public class CancelDefaultedAuctionOrderUseCase {

    static final String REASON = "PAYMENT_TIMEOUT";

    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public CancelDefaultedAuctionOrderUseCase(OrderRepositoryPort orderRepositoryPort,
                                              EventPublisherPort eventPublisherPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    // @Transactional is required: findByIdForUpdate takes a PESSIMISTIC_WRITE lock.
    @Transactional
    public void cancel(String auctionId) {
        Order order = orderRepositoryPort.findByAuctionId(auctionId)
                .flatMap(o -> orderRepositoryPort.findByIdForUpdate(o.getId()))
                .orElse(null);
        if (order == null || order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            return;
        }
        Order saved = orderRepositoryPort.save(order.cancel());
        eventPublisherPort.publish(new OrderCancelledEvent(saved.getId(), REASON, auctionId));
    }
}
