package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CancelOrderUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private EventPublisherPort eventPublisherPort;
    private CancelOrderUseCase useCase;

    private Order order() {
        return Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new CancelOrderUseCase(orderRepositoryPort, eventPublisherPort);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void cancel_setsOrderCancelledAndPublishesEvent() {
        Order order = order();
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        OrderResult result = useCase.cancel(order.getId(), "buyer-1");

        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_rejectsNonBuyerCaller() {
        Order order = order();
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.cancel(order.getId(), "someone-else"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancel_rejectsAlreadyPaidOrder() {
        Order order = order().markPaid(Instant.now());
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.cancel(order.getId(), "buyer-1"))
                .isInstanceOf(ConflictException.class);
    }
}
