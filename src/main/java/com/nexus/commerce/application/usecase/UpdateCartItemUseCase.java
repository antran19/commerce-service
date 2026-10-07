package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.CartItemNotFoundException;
import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.domain.model.Cart;

public class UpdateCartItemUseCase {

    private final CartRepositoryPort cartRepositoryPort;

    public UpdateCartItemUseCase(CartRepositoryPort cartRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
    }

    public CartResult updateQuantity(String buyerId, String productId, int newQuantity) {
        Cart cart = cartRepositoryPort.findByBuyerId(buyerId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));
        if (cart.findItem(productId).isEmpty()) {
            throw new CartItemNotFoundException(productId);
        }
        Cart updated = cart.updateItemQuantity(productId, newQuantity);
        return CartResult.from(cartRepositoryPort.save(updated));
    }
}
