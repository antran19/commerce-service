package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
import com.nexus.commerce.domain.model.Order;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.events.InventoryReservationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StartCheckoutUseCaseTest {

    private CartRepositoryPort cartRepositoryPort;
    private OrderRepositoryPort orderRepositoryPort;
    private EventPublisherPort eventPublisherPort;
    private StartCheckoutUseCase useCase;

    @BeforeEach
    void setUp() {
        cartRepositoryPort = mock(CartRepositoryPort.class);
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new StartCheckoutUseCase(cartRepositoryPort, orderRepositoryPort, eventPublisherPort);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void checkout_createsOrderAndClearsCart() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1));
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(cart));

        OrderResult result = useCase.checkout("buyer-1");

        assertThat(result.buyerId()).isEqualTo("buyer-1");
        assertThat(result.auctionId()).isNull();
        assertThat(result.totalAmount()).isEqualByComparingTo("999.00");
        verify(cartRepositoryPort).save(argThat(Cart::isEmpty));
        verify(eventPublisherPort, times(2)).publish(any());
    }

    @Test
    void checkout_publishesAnInventoryReservationRequestWithOneLineItemPerProduct() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 2));
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(cart));

        useCase.checkout("buyer-1");

        verify(eventPublisherPort).publish(argThat(e -> e instanceof InventoryReservationRequestedEvent evt
                && evt.getItems().size() == 1
                && evt.getItems().get(0).skuId().equals("product-1")
                && evt.getItems().get(0).quantity() == 2));
    }

    @Test
    void checkout_rejectsEmptyCart() {
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(Cart.create("buyer-1")));

        assertThatThrownBy(() -> useCase.checkout("buyer-1")).isInstanceOf(ConflictException.class);
        verify(orderRepositoryPort, never()).save(any());
    }
}
