package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.events.OrderPaidEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public class ConfirmPaymentUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final EventPublisherPort eventPublisherPort;

    public ConfirmPaymentUseCase(OrderRepositoryPort orderRepositoryPort, PaymentGatewayPort paymentGatewayPort,
                                  EventPublisherPort eventPublisherPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public OrderResult confirm(String orderId, String callerId, String sessionId) {
        Order order = orderRepositoryPort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getBuyerId().equals(callerId)) {
            throw new ForbiddenException("NOT_THE_BUYER", "Only the order's buyer can confirm this payment");
        }

        // Idempotent: a page refresh or duplicate confirm call after the first success must
        // not re-hit Stripe or fail -- it just reports the already-settled state back.
        if (order.getStatus() == OrderStatus.PAID) {
            return OrderResult.from(order);
        }

        PaymentGatewayPort.PaymentStatus paymentStatus = paymentGatewayPort.retrieveSessionStatus(sessionId);
        if (!paymentStatus.paid()) {
            throw new ConflictException("PAYMENT_NOT_CONFIRMED", "Stripe has not confirmed this payment yet");
        }
        // The session's own metadata must point back at this exact order/buyer -- a caller
        // cannot pay off someone else's order by passing an unrelated sessionId that happens
        // to be "paid" (e.g. one from an order they genuinely paid for).
        if (!orderId.equals(paymentStatus.orderId()) || !callerId.equals(paymentStatus.buyerId())) {
            throw new ConflictException("SESSION_ORDER_MISMATCH",
                    "This payment session does not belong to this order/buyer");
        }

        Order saved = orderRepositoryPort.save(order.markPaid(Instant.now()));
        eventPublisherPort.publish(new OrderPaidEvent(saved.getId(), saved.getAuctionId(), saved.getBuyerId(),
                saved.singleSellerIdOrNull(), saved.getTotalAmount(), saved.getPaidAt()));
        return OrderResult.from(saved);
    }
}
