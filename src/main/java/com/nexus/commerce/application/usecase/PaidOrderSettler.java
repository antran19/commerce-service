package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.common.events.OrderPaidEvent;
import com.nexus.common.events.OrderRefundedEvent;

import java.time.Instant;

// The one place that decides what a payment Stripe has confirmed does to an order. Shared by
// the buyer's browser (ConfirmPaymentUseCase) and Stripe's own webhook
// (HandlePaymentWebhookUseCase): whichever arrives first settles the order, the other sees the
// settled state and does nothing. The caller must already hold the order's row lock
// (findByIdForUpdate inside a transaction), otherwise both could settle at once.
public class PaidOrderSettler {

    static final String LATE_PAYMENT_REASON = "PAID_AFTER_CANCELLATION";

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final EventPublisherPort eventPublisherPort;

    public PaidOrderSettler(OrderRepositoryPort orderRepositoryPort, PaymentGatewayPort paymentGatewayPort,
                            EventPublisherPort eventPublisherPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    public Order settle(Order lockedOrder, String paymentReference) {
        return switch (lockedOrder.getStatus()) {
            // Already settled (by the other path, a Stripe retry, or an admin refund): no-op.
            case PAID, REFUNDED -> lockedOrder;
            case AWAITING_PAYMENT -> {
                Order saved = orderRepositoryPort.save(lockedOrder.markPaid(Instant.now(), paymentReference));
                eventPublisherPort.publish(new OrderPaidEvent(saved.getId(), saved.getAuctionId(), saved.getBuyerId(),
                        saved.singleSellerIdOrNull(), saved.getTotalAmount(), saved.getPaidAt()));
                yield saved;
            }
            // Paid after we cancelled (stock already released, maybe the buyer already
            // penalized): we can't deliver, so the money goes straight back. If the refund
            // call fails the exception rolls everything back and the webhook is retried.
            case CANCELLED -> {
                paymentGatewayPort.refund(paymentReference);
                Order saved = orderRepositoryPort.save(lockedOrder.refundLatePayment(Instant.now(), paymentReference));
                eventPublisherPort.publish(new OrderRefundedEvent(saved.getId(), saved.getBuyerId(),
                        saved.getTotalAmount(), LATE_PAYMENT_REASON, false));
                yield saved;
            }
        };
    }
}
