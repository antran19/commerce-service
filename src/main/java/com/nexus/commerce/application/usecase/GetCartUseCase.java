package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.domain.model.Cart;

public class GetCartUseCase {

    private final CartRepositoryPort cartRepositoryPort;

    public GetCartUseCase(CartRepositoryPort cartRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
    }

    // A buyer with no cart yet simply has an empty one -- the SRS calls for lazy creation,
    // not a 404, since "no cart" and "empty cart" are the same thing from the buyer's view.
    public CartResult getCart(String buyerId) {
        Cart cart = cartRepositoryPort.findByBuyerId(buyerId).orElseGet(() -> Cart.create(buyerId));
        return CartResult.from(cart);
    }
}
