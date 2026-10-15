package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.events.DomainEvent;
import com.nexus.common.events.OrderCancelledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpireUnpaidOrdersUseCaseTest {

    private static final Duration TIMEOUT = Duration.ofMinutes(30);

    private OrderRepositoryPort orderRepositoryPort;
    private EventPublisherPort eventPublisherPort;
    private ExpireUnpaidOrdersUseCase useCase;

    private Order order(Instant createdAt, String auctionId, OrderStatus status) {
        return Order.reconstitute(UUID.randomUUID().toString(), "buyer-1", auctionId,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)),
                new BigDecimal("100.00"), status, createdAt, null, null, null);
    }

    private Order stored(Order order) {
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        return order;
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new ExpireUnpaidOrdersUseCase(orderRepositoryPort, eventPublisherPort, TIMEOUT);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void findDueOrderIds_asksForOrdersOlderThanTheTimeout() {
        Order due = order(Instant.now().minus(Duration.ofHours(1)), null, OrderStatus.AWAITING_PAYMENT);
        when(orderRepositoryPort.findUnpaidDirectOrdersCreatedBefore(any(Instant.class))).thenReturn(List.of(due));

        List<String> ids = useCase.findDueOrderIds();

        assertThat(ids).containsExactly(due.getId());
        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(orderRepositoryPort).findUnpaidDirectOrdersCreatedBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(Instant.now().minus(TIMEOUT).plusSeconds(5));
        assertThat(cutoff.getValue()).isAfter(Instant.now().minus(TIMEOUT).minusSeconds(5));
    }

    @Test
    void expire_cancelsAnOverdueOrderAndPublishesAPaymentTimeoutEvent() {
        Order order = stored(order(Instant.now().minus(Duration.ofHours(1)), null, OrderStatus.AWAITING_PAYMENT));

        useCase.expire(order.getId());

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(event.capture());
        assertThat(event.getValue()).isInstanceOf(OrderCancelledEvent.class);
    }

    @Test
    void expire_leavesAnOrderThatIsStillWithinTheWindow() {
        Order order = stored(order(Instant.now().minus(Duration.ofMinutes(5)), null, OrderStatus.AWAITING_PAYMENT));

        useCase.expire(order.getId());

        verify(orderRepositoryPort, never()).save(any(Order.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void expire_neverCancelsAnOrderThatWasPaidInTheMeantime() {
        Order order = stored(order(Instant.now().minus(Duration.ofHours(1)), null, OrderStatus.PAID));

        useCase.expire(order.getId());

        verify(orderRepositoryPort, never()).save(any(Order.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void expire_neverCancelsAnAuctionOrder() {
        Order order = stored(order(Instant.now().minus(Duration.ofHours(1)), "auction-1",
                OrderStatus.AWAITING_PAYMENT));

        useCase.expire(order.getId());

        verify(orderRepositoryPort, never()).save(any(Order.class));
    }

    @Test
    void expire_isANoOpWhenTheOrderNoLongerExists() {
        when(orderRepositoryPort.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        useCase.expire("missing");

        verify(orderRepositoryPort, never()).save(any(Order.class));
    }
}
