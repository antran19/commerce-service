package com.nexus.commerce.application.usecase;

import com.nexus.commerce.domain.model.Order;

// Who may read an order (and, now that it carries an address and phone number, personal data):
// its buyer, a seller who has an item in it, or staff (Admin / Support Staff, passed in as
// canViewAny by the controller). Shared by GetOrderUseCase and GetOrderInvoiceUseCase so the
// two can never drift apart.
final class OrderAccessPolicy {

    private OrderAccessPolicy() {
    }

    static boolean canView(Order order, String callerId, boolean canViewAny) {
        if (canViewAny) {
            return true;
        }
        boolean isBuyer = order.getBuyerId().equals(callerId);
        boolean isSellerOfAnItem = order.getItems().stream().anyMatch(item -> item.getSellerId().equals(callerId));
        return isBuyer || isSellerOfAnItem;
    }
}
