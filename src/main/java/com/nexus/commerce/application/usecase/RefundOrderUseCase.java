package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.events.OrderRefundedEvent;
import org.springframework.transaction.annotation.Transactional;

// ORDER.REFUND (Admin/Support Staff only, per the SRS privilege table). Money only -- the
// physical goods are a separate, manual concern (see Order.refund() and HANDOFF.md): this
// service has no shipment tracking yet, so it can't safely decide on its own whether the
// item should go back into stock.
public class RefundOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final EventPublisherPort eventPublisherPort;

    public RefundOrderUseCase(OrderRepositoryPort orderRepositoryPort, PaymentGatewayPort paymentGatewayPort,
                               EventPublisherPort eventPublisherPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public OrderResult refund(String orderId, String reason) {
        Order order = orderRepositoryPort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Idempotent: refunding an already-refunded order is a no-op, not an error --
        // same convention as elsewhere in this codebase (ReleaseReservationUseCase, etc.).
        if (order.getStatus() == OrderStatus.REFUNDED) {
            return OrderResult.from(order);
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ConflictException("ORDER_NOT_REFUNDABLE", "Order " + orderId + " is not in a refundable state");
        }

        paymentGatewayPort.refund(order.getPaymentReference());

        Order saved = orderRepositoryPort.save(order.refund());
        eventPublisherPort.publish(new OrderRefundedEvent(saved.getId(), saved.getBuyerId(), saved.getTotalAmount(),
                reason));
        return OrderResult.from(saved);
    }
}
