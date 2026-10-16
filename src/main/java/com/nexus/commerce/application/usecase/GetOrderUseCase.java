package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.common.core.exception.ForbiddenException;

public class GetOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public GetOrderUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    // An order carries the buyer's address and phone number, so it is only readable by the
    // buyer, a seller in the order, or staff (see OrderAccessPolicy).
    public OrderResult getOrder(String orderId, String callerId, boolean canViewAny) {
        Order order = orderRepositoryPort.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!OrderAccessPolicy.canView(order, callerId, canViewAny)) {
            throw new ForbiddenException("NOT_ALLOWED_TO_VIEW_ORDER",
                    "Only the buyer, a seller in the order, or staff may view this order");
        }
        return OrderResult.from(order);
    }
}
