package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.CartItemNotFoundException;
import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.ProductStockRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateCartItemUseCaseTest {

    private CartRepositoryPort cartRepositoryPort;
    private ProductStockRepositoryPort productStockRepositoryPort;
    private UpdateCartItemUseCase useCase;

    @BeforeEach
    void setUp() {
        cartRepositoryPort = mock(CartRepositoryPort.class);
        productStockRepositoryPort = mock(ProductStockRepositoryPort.class);
        useCase = new UpdateCartItemUseCase(cartRepositoryPort, productStockRepositoryPort);
        when(productStockRepositoryPort.availableQuantity("product-1")).thenReturn(10);
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void updateQuantity_setsNewQuantity() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1));
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(cart));

        CartResult result = useCase.updateQuantity("buyer-1", "product-1", 5);

        assertThat(result.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void updateQuantity_rejectsUnknownProduct() {
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(Cart.create("buyer-1")));

        assertThatThrownBy(() -> useCase.updateQuantity("buyer-1", "product-404", 1))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void updateQuantity_rejectsWhenBuyerHasNoCart() {
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateQuantity("buyer-1", "product-1", 1))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void updateQuantity_rejectsMoreThanAvailableStock() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1));
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> useCase.updateQuantity("buyer-1", "product-1", 11))
                .isInstanceOf(com.nexus.common.core.exception.ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INSUFFICIENT_STOCK");
        verify(cartRepositoryPort, never()).save(any());
    }
}
