package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Stripe calls this itself once a checkout session is paid, so an order no longer depends on
// the buyer's browser making it back to confirm-payment (closed tab, lost connection). Stripe
// retries until it gets a 2xx and may deliver the same event more than once -- PaidOrderSettler
// under the row lock makes repeats harmless. Anything we can't act on (not paid yet, an order
// that isn't ours) returns normally, so Stripe stops retrying something that will never work.
public class HandlePaymentWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandlePaymentWebhookUseCase.class);

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaidOrderSettler paidOrderSettler;

    public HandlePaymentWebhookUseCase(OrderRepositoryPort orderRepositoryPort, PaymentGatewayPort paymentGatewayPort,
                                        PaidOrderSettler paidOrderSettler) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.paidOrderSettler = paidOrderSettler;
    }

    @Transactional
    public void handle(String payload, String signatureHeader) {
        Optional<String> sessionId = paymentGatewayPort.verifyWebhookAndExtractSessionId(payload, signatureHeader);
        if (sessionId.isEmpty()) {
            return;
        }

        // Re-read the session from Stripe rather than trusting the event body: same source of
        // truth confirm-payment uses, and it covers a delayed method that isn't paid yet.
        PaymentGatewayPort.PaymentStatus status = paymentGatewayPort.retrieveSessionStatus(sessionId.get());
        if (!status.paid() || status.orderId() == null) {
            log.info("Ignoring webhook for session {}: not paid yet or no orderId metadata", sessionId.get());
            return;
        }

        Order order = orderRepositoryPort.findByIdForUpdate(status.orderId()).orElse(null);
        // e.g. another environment sharing the same Stripe test account
        if (order == null || !order.getBuyerId().equals(status.buyerId())) {
            log.warn("Ignoring webhook for session {}: order {} not found or buyer mismatch",
                    sessionId.get(), status.orderId());
            return;
        }

        paidOrderSettler.settle(order, status.paymentReference());
    }
}
