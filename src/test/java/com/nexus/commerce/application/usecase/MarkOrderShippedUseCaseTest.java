package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
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

class MarkOrderShippedUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private MarkOrderShippedUseCase useCase;

    private Order paidOrderForSeller(String sellerId) {
        Order order = Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", sellerId, "Laptop X", new BigDecimal("100.00"), 1)));
        return order.markPaid(Instant.now(), "pi_123");
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        useCase = new MarkOrderShippedUseCase(orderRepositoryPort);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void markShipped_setsShippedAtForTheOwningSeller() {
        Order order = paidOrderForSeller("seller-1");
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        OrderResult result = useCase.markShipped(order.getId(), "seller-1");

        assertThat(result.shippedAt()).isNotNull();
    }

    @Test
    void markShipped_rejectsCallerWhoIsNotASellerInTheOrder() {
        Order order = paidOrderForSeller("seller-1");
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.markShipped(order.getId(), "someone-else"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void markShipped_rejectsAnOrderThatIsNotPaid() {
        Order order = Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.markShipped(order.getId(), "seller-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void markShipped_rejectsAnOrderAlreadyMarkedShipped() {
        Order order = paidOrderForSeller("seller-1").markShipped(Instant.now());
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.markShipped(order.getId(), "seller-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void markShipped_throwsForAnUnknownOrder() {
        when(orderRepositoryPort.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.markShipped("missing", "seller-1"))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
