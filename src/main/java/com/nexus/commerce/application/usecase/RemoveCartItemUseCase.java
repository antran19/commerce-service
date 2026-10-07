package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.CartItemNotFoundException;
import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.domain.model.Cart;

public class RemoveCartItemUseCase {

    private final CartRepositoryPort cartRepositoryPort;

    public RemoveCartItemUseCase(CartRepositoryPort cartRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
    }

    public CartResult removeItem(String buyerId, String productId) {
        Cart cart = cartRepositoryPort.findByBuyerId(buyerId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));
        if (cart.findItem(productId).isEmpty()) {
            throw new CartItemNotFoundException(productId);
        }
        Cart updated = cart.removeItem(productId);
        return CartResult.from(cartRepositoryPort.save(updated));
    }
}
