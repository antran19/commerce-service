package com.nexus.commerce.application.port.out;

import java.time.Instant;

public interface ProductStockRepositoryPort {
    // 0 for a product whose stock was never reported: nothing has been taken in for it yet.
    int availableQuantity(String productId);

    // Stores the level unless a level reported at a later time is already stored, so a
    // redelivered or out-of-order event cannot roll the stock back.
    void applyIfNewer(String productId, int availableQuantity, Instant asOf);
}
