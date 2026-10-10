package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.common.events.OrderCancelledEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CancelOrderDueToStockFailureUseCaseTest {

    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final EventPublisherPort eventPublisherPort = mock(EventPublisherPort.class);
    private final CancelOrderDueToStockFailureUseCase useCase =
            new CancelOrderDueToStockFailureUseCase(orderRepositoryPort, eventPublisherPort);

    private Order order(String auctionId) {
        return Order.create("buyer-1", auctionId,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
    }

    @Test
    void cancel_marksTheOrderCancelled_andPublishesOrderCancelledWithTheAuctionId() {
        Order order = order("auction-1");
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.cancel(order.getId(), "OUT_OF_STOCK");

        verify(orderRepositoryPort).save(argThat(o -> o.getStatus().name().equals("CANCELLED")));
        verify(eventPublisherPort).publish(argThat(e -> e instanceof OrderCancelledEvent evt
                && evt.getOrderId().equals(order.getId()) && evt.getAuctionId().equals("auction-1")));
    }

    @Test
    void cancel_isANoOpWhenOrderIsAlreadyPaid() {
        Order paid = order(null).markPaid(Instant.now());
        when(orderRepositoryPort.findByIdForUpdate(paid.getId())).thenReturn(Optional.of(paid));

        useCase.cancel(paid.getId(), "OUT_OF_STOCK");

        verify(orderRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void cancel_isANoOpForAnUnknownOrder() {
        when(orderRepositoryPort.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        useCase.cancel("missing", "OUT_OF_STOCK");

        verify(eventPublisherPort, never()).publish(any());
    }
}
