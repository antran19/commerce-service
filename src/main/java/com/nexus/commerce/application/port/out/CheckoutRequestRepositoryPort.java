package com.nexus.commerce.application.port.out;

import java.time.Instant;
import java.util.Optional;

// Which order did this buyer's checkout request (identified by its Idempotency-Key) create?
public interface CheckoutRequestRepositoryPort {
    Optional<String> findOrderId(String buyerId, String idempotencyKey);

    void save(String buyerId, String idempotencyKey, String orderId);

    // Returns how many rows were removed.
    int deleteOlderThan(Instant cutoff);
}
