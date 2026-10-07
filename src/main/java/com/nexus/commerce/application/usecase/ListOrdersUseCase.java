package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.OrderRepositoryPort;

import java.util.List;

public class ListOrdersUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public ListOrdersUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public List<OrderResult> listForBuyer(String buyerId) {
        return orderRepositoryPort.findByBuyerId(buyerId).stream().map(OrderResult::from).toList();
    }
}
