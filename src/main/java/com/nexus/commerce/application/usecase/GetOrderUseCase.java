package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;

public class GetOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public GetOrderUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public OrderResult getOrder(String orderId) {
        return OrderResult.from(orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId)));
    }
}
