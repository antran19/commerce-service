package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.ProductSnapshot;
import com.nexus.common.events.InventoryReservationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CreateOrderFromAuctionUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private EventPublisherPort eventPublisherPort;
    private ProductSnapshotRepositoryPort productSnapshotRepositoryPort;
    private CreateOrderFromAuctionUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        productSnapshotRepositoryPort = mock(ProductSnapshotRepositoryPort.class);
        when(productSnapshotRepositoryPort.findById(any())).thenReturn(Optional.empty());
        useCase = new CreateOrderFromAuctionUseCase(orderRepositoryPort, eventPublisherPort, productSnapshotRepositoryPort);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createFromAuction_createsOrderWithAuctionIdAndWinnerAsBuyer() {
        when(orderRepositoryPort.findByAuctionId("auction-1")).thenReturn(Optional.empty());

        OrderResult result = useCase.createFromAuction("auction-1", "product-1", "seller-1",
                "winner-1", new BigDecimal("150.00"));

        assertThat(result.auctionId()).isEqualTo("auction-1");
        assertThat(result.buyerId()).isEqualTo("winner-1");
        assertThat(result.totalAmount()).isEqualByComparingTo("150.00");
        verify(eventPublisherPort, times(1)).publish(any());
    }

    @Test
    void createFromAuction_namesTheOrderLineAfterTheProduct_whenTheCatalogCopyKnowsIt() {
        when(orderRepositoryPort.findByAuctionId("auction-1")).thenReturn(Optional.empty());
        when(productSnapshotRepositoryPort.findById("product-1")).thenReturn(Optional.of(
                ProductSnapshot.create("product-1", "seller-1", "Ví da Nexus", new BigDecimal("320000"))));

        OrderResult result = useCase.createFromAuction("auction-1", "product-1", "seller-1",
                "winner-1", new BigDecimal("150.00"));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productName()).isEqualTo("Ví da Nexus");
    }

    @Test
    void createFromAuction_fallsBackToAGenericLabel_whenTheCatalogCopyDoesNotKnowTheProduct() {
        when(orderRepositoryPort.findByAuctionId("auction-1")).thenReturn(Optional.empty());

        OrderResult result = useCase.createFromAuction("auction-1", "product-1", "seller-1",
                "winner-1", new BigDecimal("150.00"));

        assertThat(result.items().get(0).productName()).isEqualTo("Auction item product-1");
    }

    @Test
    void createFromAuction_doesNotAskForStock_theUnitIsAlreadyHeldForTheAuction() {
        // fulfillment has held the unit since the auction was created; asking again would hold a
        // second one (or fail, the only unit being held) and cancel the winner's order.
        when(orderRepositoryPort.findByAuctionId("auction-1")).thenReturn(Optional.empty());

        useCase.createFromAuction("auction-1", "product-1", "seller-1", "winner-1", new BigDecimal("150.00"));

        verify(eventPublisherPort, never()).publish(argThat(e -> e instanceof InventoryReservationRequestedEvent));
    }

    @Test
    void createFromAuction_isIdempotentWhenOrderAlreadyExistsForAuction() {
        Order existing = Order.create("winner-1", "auction-1",
                List.of(new OrderItem("product-1", "seller-1", "Auction item product-1",
                        new BigDecimal("150.00"), 1)));
        when(orderRepositoryPort.findByAuctionId("auction-1")).thenReturn(Optional.of(existing));

        OrderResult result = useCase.createFromAuction("auction-1", "product-1", "seller-1",
                "winner-1", new BigDecimal("150.00"));

        assertThat(result.id()).isEqualTo(existing.getId());
        verify(orderRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }
}
