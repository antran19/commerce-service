package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.ProductStockRepositoryPort;
import com.nexus.common.core.exception.ConflictException;

// Shared by add-to-cart, change-quantity and checkout so all three refuse the same way.
// The stock copy lags fulfillment by a few seconds, so this is an early warning for the buyer,
// not the guarantee: fulfillment's own conditional reservation still decides who gets the
// last unit, and an order it cannot reserve is cancelled as before.
final class StockGuard {

    private StockGuard() {
    }

    static void requireAvailable(ProductStockRepositoryPort stockPort, String productId, int wantedQuantity) {
        int available = stockPort.availableQuantity(productId);
        if (wantedQuantity > available) {
            throw new ConflictException("INSUFFICIENT_STOCK",
                    "Only " + available + " left of product " + productId + ", requested " + wantedQuantity);
        }
    }
}
