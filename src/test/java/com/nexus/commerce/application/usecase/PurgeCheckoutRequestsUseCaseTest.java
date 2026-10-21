package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CheckoutRequestRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurgeCheckoutRequestsUseCaseTest {

    private final CheckoutRequestRepositoryPort port = mock(CheckoutRequestRepositoryPort.class);
    private final PurgeCheckoutRequestsUseCase useCase = new PurgeCheckoutRequestsUseCase(port, Duration.ofHours(24));

    @Test
    void purge_deletesRecordsOlderThanTheRetentionWindow() {
        when(port.deleteOlderThan(Instant.parse("2026-10-20T10:00:00Z"))).thenReturn(4);

        assertThat(useCase.purgeAt(Instant.parse("2026-10-21T10:00:00Z"))).isEqualTo(4);

        verify(port).deleteOlderThan(Instant.parse("2026-10-20T10:00:00Z"));
    }
}
