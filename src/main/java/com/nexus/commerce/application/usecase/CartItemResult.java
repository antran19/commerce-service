package com.nexus.commerce.application.usecase;

import com.nexus.commerce.domain.model.CartItem;

import java.math.BigDecimal;

public record CartItemResult(String productId, String sellerId, String productName,
                              BigDecimal unitPrice, int quantity, BigDecimal subtotal) {

    public static CartItemResult from(CartItem item) {
        return new CartItemResult(item.getProductId(), item.getSellerId(), item.getProductName(),
                item.getUnitPrice(), item.getQuantity(), item.subtotal());
    }
}
