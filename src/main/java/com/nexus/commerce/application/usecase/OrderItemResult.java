package com.nexus.commerce.application.usecase;

import com.nexus.commerce.domain.model.OrderItem;

import java.math.BigDecimal;

public record OrderItemResult(String productId, String sellerId, String productName,
                               BigDecimal unitPrice, int quantity, BigDecimal subtotal) {

    public static OrderItemResult from(OrderItem item) {
        return new OrderItemResult(item.getProductId(), item.getSellerId(), item.getProductName(),
                item.getUnitPrice(), item.getQuantity(), item.subtotal());
    }
}
