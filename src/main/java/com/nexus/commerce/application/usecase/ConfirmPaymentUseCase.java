package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;
import org.springframework.transaction.annotation.Transactional;

// The buyer's browser path back from Stripe. Stripe's webhook (HandlePaymentWebhookUseCase)
// settles the same order even if the buyer never comes back; both go through PaidOrderSettler.
public class ConfirmPaymentUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaidOrderSettler paidOrderSettler;

    public ConfirmPaymentUseCase(OrderRepositoryPort orderRepositoryPort, PaymentGatewayPort paymentGatewayPort,
                                  PaidOrderSettler paidOrderSettler) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.paidOrderSettler = paidOrderSettler;
    }

    @Transactional
    public OrderResult confirm(String orderId, String callerId, String sessionId) {
        Order order = orderRepositoryPort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getBuyerId().equals(callerId)) {
            throw new ForbiddenException("NOT_THE_BUYER", "Only the order's buyer can confirm this payment");
        }

        // Idempotent: a page refresh, a duplicate confirm call, or the webhook having already
        // settled the order must not re-hit Stripe or fail -- just report the settled state.
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.REFUNDED) {
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

        // AWAITING_PAYMENT -> PAID; CANCELLED -> refunded straight away (paid too late).
        return OrderResult.from(paidOrderSettler.settle(order, paymentStatus.paymentReference()));
    }
}
