package com.nexus.commerce.infrastructure.payment;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StripePaymentGatewayAdapterTest {

    @Test
    void expiresAt_isWithinStripesAllowedWindowOfThirtyMinutesToTwentyFourHours() {
        Instant now = Instant.parse("2026-10-15T03:00:00Z");

        long seconds = StripePaymentGatewayAdapter.expiresAt(now) - now.getEpochSecond();

        assertThat(seconds).isGreaterThanOrEqualTo(Duration.ofMinutes(30).toSeconds());
        assertThat(seconds).isLessThanOrEqualTo(Duration.ofHours(24).toSeconds());
    }
}
