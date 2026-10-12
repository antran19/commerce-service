package com.nexus.commerce.application.port.out;

import com.nexus.commerce.domain.model.Cart;

import java.time.Instant;
import java.util.Optional;

public interface CartRepositoryPort {
    Optional<Cart> findByBuyerId(String buyerId);
    Cart save(Cart cart);

    // Returns how many carts were removed.
    int deleteInactiveSince(Instant cutoff);
}
