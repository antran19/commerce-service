package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.OrderRepositoryPort;

import java.util.List;

// Admin/Support Staff side of ORDER.LIST (the same privilege code Buyer/Seller use for
// listForBuyer): the SRS grants ORDER.LIST to all 4 roles, but V9's seeding comment says
// support staff "can view/list/cancel any order" -- this is that "any order" scope. The
// controller is responsible for only calling this for ADMIN/SUPPORT_STAFF callers.
public class ListAllOrdersUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public ListAllOrdersUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public List<OrderResult> list() {
        return orderRepositoryPort.findAll().stream().map(OrderResult::from).toList();
    }
}
