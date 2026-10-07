package com.nexus.commerce.application.port.out;

import com.nexus.commerce.domain.model.Cart;

import java.util.Optional;

public interface CartRepositoryPort {
    Optional<Cart> findByBuyerId(String buyerId);
    Cart save(Cart cart);
}
