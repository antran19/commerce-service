package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.events.OrderRefundedEvent;
import com.nexus.common.events.OrderReturnReceivedEvent;
import com.nexus.common.events.ReservationLineItem;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ORDER.REFUND (Admin/Support Staff only, per the SRS privilege table). Money always; goods
// depend on shippedAt: an order that never shipped has its reservation already COMMITTED
// (not just held) by the time it's PAID -- commit() permanently removes the stock from
// total_quantity, so a plain release can't undo it (ReleaseReservationUseCase only ever
// un-reserves a still-PENDING hold). Putting stock back needs the same restock path the
// manual Return flow uses (OrderReturnReceivedEvent -> IntakeInventoryUseCase), published
// immediately here since there's nothing left to confirm. An already-shipped order's goods
// stay manual -- see ReturnRequest/ReceiveReturnUseCase.
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
        boolean shipped = saved.getShippedAt() != null;
        eventPublisherPort.publish(new OrderRefundedEvent(saved.getId(), saved.getBuyerId(), saved.getTotalAmount(),
                reason, shipped));
        if (!shipped) {
            eventPublisherPort.publish(new OrderReturnReceivedEvent(saved.getId(), toLineItems(saved.getItems())));
        }
        return OrderResult.from(saved);
    }

    private static List<ReservationLineItem> toLineItems(List<OrderItem> items) {
        return items.stream().map(i -> new ReservationLineItem(i.getProductId(), i.getQuantity())).toList();
    }
}
