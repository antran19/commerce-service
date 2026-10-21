package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CheckoutRequestRepositoryPort;

import java.time.Duration;
import java.time.Instant;

// An Idempotency-Key only matters while the client might still retry that request (seconds to
// minutes), so records older than the retention window are removed to keep the table small.
public class PurgeCheckoutRequestsUseCase {

    private final CheckoutRequestRepositoryPort checkoutRequestRepositoryPort;
    private final Duration retention;

    public PurgeCheckoutRequestsUseCase(CheckoutRequestRepositoryPort checkoutRequestRepositoryPort,
                                         Duration retention) {
        this.checkoutRequestRepositoryPort = checkoutRequestRepositoryPort;
        this.retention = retention;
    }

    public int purge() {
        return purgeAt(Instant.now());
    }

    int purgeAt(Instant now) {
        return checkoutRequestRepositoryPort.deleteOlderThan(now.minus(retention));
    }
}
