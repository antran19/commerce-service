package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.events.OrderCancelledEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Direct-checkout orders only (auctionId == null): an auction order has its own 24h payment
// deadline and a reputation penalty, handled by auction-service's PaymentDeadlineJob, so this
// must never cancel those. Two methods rather than one so that each order is expired in its
// own transaction (the job calls expire() per id, like AuctionLifecycleJob does) -- a
// self-call inside one @Transactional method would not get a transaction per order.
public class ExpireUnpaidOrdersUseCase {

    static final String REASON = "PAYMENT_TIMEOUT";

    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;
    private final Duration timeout;

    public ExpireUnpaidOrdersUseCase(OrderRepositoryPort orderRepositoryPort, EventPublisherPort eventPublisherPort,
                                      Duration timeout) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.timeout = timeout;
    }

    public List<String> findDueOrderIds() {
        Instant cutoff = Instant.now().minus(timeout);
        return orderRepositoryPort.findUnpaidDirectOrdersCreatedBefore(cutoff).stream().map(Order::getId).toList();
    }

    // @Transactional is required: findByIdForUpdate takes a PESSIMISTIC_WRITE lock.
    @Transactional
    public void expire(String orderId) {
        Order order = orderRepositoryPort.findByIdForUpdate(orderId).orElse(null);
        // Re-check everything under the lock: the buyer may have paid or cancelled between the
        // job's list query and now, and a paid order must never be cancelled here.
        if (order == null || order.getStatus() != OrderStatus.AWAITING_PAYMENT || order.getAuctionId() != null) {
            return;
        }
        if (order.getCreatedAt().isAfter(Instant.now().minus(timeout))) {
            return;
        }
        Order saved = orderRepositoryPort.save(order.cancel());
        eventPublisherPort.publish(new OrderCancelledEvent(saved.getId(), REASON, null));
    }
}
