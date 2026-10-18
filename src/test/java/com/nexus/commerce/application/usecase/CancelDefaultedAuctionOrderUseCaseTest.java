package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.events.OrderCancelledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CancelDefaultedAuctionOrderUseCaseTest {

    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final EventPublisherPort eventPublisherPort = mock(EventPublisherPort.class);
    private final CancelDefaultedAuctionOrderUseCase useCase =
            new CancelDefaultedAuctionOrderUseCase(orderRepositoryPort, eventPublisherPort);

    private final Order awaiting = Order.create("winner-1", "auction-1",
            List.of(new OrderItem("product-1", "seller-1", "Auction item", new BigDecimal("150.00"), 1)));

    @BeforeEach
    void setUp() {
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepositoryPort.findByAuctionId("auction-1")).thenReturn(Optional.of(awaiting));
        when(orderRepositoryPort.findByIdForUpdate(awaiting.getId())).thenReturn(Optional.of(awaiting));
    }

    @Test
    void cancel_theWinnersUnpaidOrder_soItCanNoLongerBePaidLate() {
        useCase.cancel("auction-1");

        verify(orderRepositoryPort).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
        verify(eventPublisherPort).publish(argThat(e -> e instanceof OrderCancelledEvent evt
                && evt.getOrderId().equals(awaiting.getId()) && "PAYMENT_TIMEOUT".equals(evt.getReason())
                && "auction-1".equals(evt.getAuctionId())));
    }

    @Test
    void cancel_isANoOpWhenTheOrderIsAlreadyCancelledPaidOrMissing() {
        // The winner cancelling it themselves is what usually triggers this event.
        when(orderRepositoryPort.findByIdForUpdate(awaiting.getId())).thenReturn(Optional.of(awaiting.cancel()));
        when(orderRepositoryPort.findByAuctionId("no-order")).thenReturn(Optional.empty());

        useCase.cancel("auction-1");
        useCase.cancel("no-order");

        verify(orderRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }
}
