package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.exception.ReturnRequestNotFoundException;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.ReturnRequest;
import com.nexus.commerce.domain.model.ReturnRequestStatus;
import com.nexus.common.events.OrderReturnReceivedEvent;
import com.nexus.common.events.ReservationLineItem;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ORDER.REFUND (Admin/Support Staff). Confirms the physical goods came back and tells
// fulfillment-service to restock them -- see CommerceEventsListener (fulfillment-service)
// for the IntakeForReturnUseCase on the other end.
public class ReceiveReturnUseCase {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public ReceiveReturnUseCase(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                                 OrderRepositoryPort orderRepositoryPort, EventPublisherPort eventPublisherPort) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public ReturnRequestResult receive(String returnRequestId, String adminId) {
        ReturnRequest returnRequest = returnRequestRepositoryPort.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new ReturnRequestNotFoundException(returnRequestId));

        // Idempotent: receiving an already-received return is a no-op, same convention as
        // RefundOrderUseCase -- prevents a double restock from a retried/duplicate call.
        if (returnRequest.getStatus() == ReturnRequestStatus.RECEIVED) {
            return ReturnRequestResult.from(returnRequest);
        }

        Order order = orderRepositoryPort.findById(returnRequest.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(returnRequest.getOrderId()));

        ReturnRequest saved = returnRequestRepositoryPort.save(returnRequest.receive(adminId));
        eventPublisherPort.publish(new OrderReturnReceivedEvent(order.getId(), toLineItems(order.getItems())));
        return ReturnRequestResult.from(saved);
    }

    private static List<ReservationLineItem> toLineItems(List<OrderItem> items) {
        return items.stream().map(i -> new ReservationLineItem(i.getProductId(), i.getQuantity())).toList();
    }
}
