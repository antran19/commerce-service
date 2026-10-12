package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpireInactiveCartsUseCaseTest {

    private final CartRepositoryPort cartRepositoryPort = mock(CartRepositoryPort.class);
    private final ExpireInactiveCartsUseCase useCase =
            new ExpireInactiveCartsUseCase(cartRepositoryPort, Duration.ofDays(30));

    @Test
    void expire_deletesCartsOlderThanTheMaxInactivity() {
        when(cartRepositoryPort.deleteInactiveSince(org.mockito.ArgumentMatchers.any())).thenReturn(3);
        Instant before = Instant.now();

        int deleted = useCase.expire();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(cartRepositoryPort).deleteInactiveSince(cutoff.capture());
        assertThat(deleted).isEqualTo(3);
        assertThat(cutoff.getValue()).isBetween(before.minus(Duration.ofDays(30)).minusSeconds(1),
                Instant.now().minus(Duration.ofDays(30)).plusSeconds(1));
    }
}
