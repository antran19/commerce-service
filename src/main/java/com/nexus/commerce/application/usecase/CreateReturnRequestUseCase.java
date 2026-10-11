package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.commerce.domain.model.ReturnRequest;
import com.nexus.commerce.domain.model.ReturnRequestStatus;
import com.nexus.common.core.exception.ConflictException;

// ORDER.REFUND (Admin/Support Staff -- same actor set already handling refunds, no new
// privilege). Only makes sense for an order that was shipped (markShipped) AND already
// refunded: an order refunded before shipping is auto-released by RefundOrderUseCase
// already, nothing to return.
public class CreateReturnRequestUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;

    public CreateReturnRequestUseCase(OrderRepositoryPort orderRepositoryPort,
                                       ReturnRequestRepositoryPort returnRequestRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
    }

    public ReturnRequestResult create(String orderId, String reason) {
        Order order = orderRepositoryPort.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.REFUNDED || order.getShippedAt() == null) {
            throw new ConflictException("RETURN_NOT_APPLICABLE",
                    "Order " + orderId + " must be refunded and shipped before a return can be requested");
        }
        boolean alreadyRequested = returnRequestRepositoryPort.findByOrderId(orderId).stream()
                .anyMatch(r -> r.getStatus() == ReturnRequestStatus.REQUESTED);
        if (alreadyRequested) {
            throw new ConflictException("RETURN_ALREADY_REQUESTED",
                    "Order " + orderId + " already has a pending return request");
        }

        ReturnRequest saved = returnRequestRepositoryPort.save(ReturnRequest.create(orderId, reason));
        return ReturnRequestResult.from(saved);
    }
}
