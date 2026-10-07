package com.nexus.commerce.application.usecase;

import com.nexus.commerce.domain.model.Cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResult(String id, String buyerId, List<CartItemResult> items, BigDecimal totalAmount) {

    public static CartResult from(Cart cart) {
        List<CartItemResult> items = cart.getItems().stream().map(CartItemResult::from).toList();
        return new CartResult(cart.getId(), cart.getBuyerId(), items, cart.totalAmount());
    }
}
