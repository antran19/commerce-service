package com.nexus.commerce.application.port.out;

import com.nexus.commerce.domain.model.Cart;

import java.time.Instant;
import java.util.Optional;

public interface CartRepositoryPort {
    Optional<Cart> findByBuyerId(String buyerId);
    // Locks the buyer's cart row until the surrounding transaction ends. Checkout uses it so two
    // simultaneous requests (a double click) are handled one after the other, not both at once.
    // Only meaningful inside a transaction.
    Optional<Cart> findByBuyerIdForUpdate(String buyerId);
    Cart save(Cart cart);

    // Returns how many carts were removed.
    int deleteInactiveSince(Instant cutoff);
}
