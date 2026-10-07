package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddCartItemUseCaseTest {

    private CartRepositoryPort cartRepositoryPort;
    private AddCartItemUseCase useCase;

    @BeforeEach
    void setUp() {
        cartRepositoryPort = mock(CartRepositoryPort.class);
        useCase = new AddCartItemUseCase(cartRepositoryPort);
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addItem_createsCartWhenBuyerHasNone() {
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.empty());

        CartResult result = useCase.addItem("buyer-1", "product-1", "seller-1", "Laptop X",
                new BigDecimal("999.00"), 1);

        assertThat(result.buyerId()).isEqualTo("buyer-1");
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalAmount()).isEqualByComparingTo("999.00");
    }

    @Test
    void addItem_mergesIntoExistingCart() {
        Cart existing = Cart.create("buyer-1")
                .addItem(com.nexus.commerce.domain.model.CartItem.create(
                        "product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1));
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(existing));

        CartResult result = useCase.addItem("buyer-1", "product-1", "seller-1", "Laptop X",
                new BigDecimal("999.00"), 2);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(3);
    }
}
