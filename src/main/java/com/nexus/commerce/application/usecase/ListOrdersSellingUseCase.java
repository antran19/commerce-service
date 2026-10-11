package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.OrderRepositoryPort;

import java.util.List;

// Seller side of ORDER.LIST (same privilege code Buyer uses for listForBuyer, and
// Admin/Support Staff use for ListAllOrdersUseCase) -- orders that contain at least one
// item sold by this seller, not just orders the seller themselves placed.
public class ListOrdersSellingUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public ListOrdersSellingUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public List<OrderResult> listForSeller(String sellerId) {
        return orderRepositoryPort.findBySellerId(sellerId).stream().map(OrderResult::from).toList();
    }
}
