package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.application.port.out.ProductStockRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.ProductSnapshot;
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
    private ProductSnapshotRepositoryPort productSnapshotRepositoryPort;
    private ProductStockRepositoryPort productStockRepositoryPort;
    private StartCheckoutUseCase useCase;

    @BeforeEach
    void setUp() {
        cartRepositoryPort = mock(CartRepositoryPort.class);
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        productSnapshotRepositoryPort = mock(ProductSnapshotRepositoryPort.class);
        productStockRepositoryPort = mock(ProductStockRepositoryPort.class);
        useCase = new StartCheckoutUseCase(cartRepositoryPort, orderRepositoryPort, eventPublisherPort,
                productSnapshotRepositoryPort, productStockRepositoryPort);
        when(productStockRepositoryPort.availableQuantity("product-1")).thenReturn(10);
        when(productStockRepositoryPort.availableQuantity("product-2")).thenReturn(10);
        when(productSnapshotRepositoryPort.findById("product-1")).thenReturn(Optional.of(
                ProductSnapshot.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"))
                        .withStatus("ACTIVE")));
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

    @Test
    void checkout_repricesItemsFromTheCatalogCopy_soAForgedCartPriceIsNotCharged() {
        // A cart filled before prices were checked server-side may hold a price the client made up.
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "someone-else", "Fake name", new BigDecimal("1.00"), 2));
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(cart));

        OrderResult result = useCase.checkout("buyer-1");

        assertThat(result.totalAmount()).isEqualByComparingTo("1998.00");
        assertThat(result.items().get(0).unitPrice()).isEqualByComparingTo("999.00");
        assertThat(result.items().get(0).sellerId()).isEqualTo("seller-1");
    }

    @Test
    void checkout_rejectsACartWithAProductNoLongerForSale_andKeepsTheCart() {
        when(productSnapshotRepositoryPort.findById("product-2")).thenReturn(Optional.of(
                ProductSnapshot.create("product-2", "seller-1", "Phone", new BigDecimal("10.00"))
                        .withStatus("INACTIVE")));
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1))
                .addItem(CartItem.create("product-2", "seller-1", "Phone", new BigDecimal("10.00"), 1));
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> useCase.checkout("buyer-1"))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CART_ITEM_UNAVAILABLE")
                .hasMessageContaining("product-2");
        verify(orderRepositoryPort, never()).save(any());
        verify(cartRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void checkout_rejectsWhenStockDroppedBelowTheCartQuantity_andKeepsTheCart() {
        // Someone else bought most of it after this buyer filled the cart.
        when(productStockRepositoryPort.availableQuantity("product-1")).thenReturn(2);
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 3));
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> useCase.checkout("buyer-1"))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INSUFFICIENT_STOCK")
                .hasMessageContaining("product-1");
        verify(orderRepositoryPort, never()).save(any());
        verify(cartRepositoryPort, never()).save(any());
    }
}
