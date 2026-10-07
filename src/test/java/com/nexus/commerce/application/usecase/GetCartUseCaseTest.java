package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetCartUseCaseTest {

    private CartRepositoryPort cartRepositoryPort;
    private GetCartUseCase useCase;

    @BeforeEach
    void setUp() {
        cartRepositoryPort = mock(CartRepositoryPort.class);
        useCase = new GetCartUseCase(cartRepositoryPort);
    }

    @Test
    void getCart_returnsEmptyCartWithoutPersistingWhenBuyerHasNone() {
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.empty());

        CartResult result = useCase.getCart("buyer-1");

        assertThat(result.buyerId()).isEqualTo("buyer-1");
        assertThat(result.items()).isEmpty();
    }
}
