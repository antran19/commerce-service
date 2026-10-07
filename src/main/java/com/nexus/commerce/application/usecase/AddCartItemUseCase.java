package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;

import java.math.BigDecimal;

public class AddCartItemUseCase {

    private final CartRepositoryPort cartRepositoryPort;

    public AddCartItemUseCase(CartRepositoryPort cartRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
    }

    public CartResult addItem(String buyerId, String productId, String sellerId, String productName,
                               BigDecimal unitPrice, int quantity) {
        Cart cart = cartRepositoryPort.findByBuyerId(buyerId).orElseGet(() -> Cart.create(buyerId));
        Cart updated = cart.addItem(CartItem.create(productId, sellerId, productName, unitPrice, quantity));
        return CartResult.from(cartRepositoryPort.save(updated));
    }
}
