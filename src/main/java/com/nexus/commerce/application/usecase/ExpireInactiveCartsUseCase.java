package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;

import java.time.Duration;
import java.time.Instant;

// SRS 3.4.1: "may expire an inactive cart after a defined period of time".
public class ExpireInactiveCartsUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final Duration maxInactivity;

    public ExpireInactiveCartsUseCase(CartRepositoryPort cartRepositoryPort, Duration maxInactivity) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.maxInactivity = maxInactivity;
    }

    public int expire() {
        return cartRepositoryPort.deleteInactiveSince(Instant.now().minus(maxInactivity));
    }
}
