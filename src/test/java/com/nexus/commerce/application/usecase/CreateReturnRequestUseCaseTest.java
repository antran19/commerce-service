package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.ReturnRequest;
import com.nexus.common.core.exception.ConflictException;
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

class CreateReturnRequestUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private CreateReturnRequestUseCase useCase;

    private Order shippedRefundedOrder() {
        Order order = Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
        return order.markPaid(Instant.now(), "pi_123").markShipped(Instant.now()).refund();
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        returnRequestRepositoryPort = mock(ReturnRequestRepositoryPort.class);
        useCase = new CreateReturnRequestUseCase(orderRepositoryPort, returnRequestRepositoryPort);
        when(returnRequestRepositoryPort.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(returnRequestRepositoryPort.findByOrderId(any())).thenReturn(List.of());
    }

    @Test
    void create_createsARequestedReturnForAShippedRefundedOrder() {
        Order order = shippedRefundedOrder();
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        ReturnRequestResult result = useCase.create(order.getId(), "Wrong item received");

        assertThat(result.status()).isEqualTo("REQUESTED");
        assertThat(result.orderId()).isEqualTo(order.getId());
    }

    @Test
    void create_rejectsAnOrderThatWasNeverShipped() {
        Order order = Order.create("buyer-1", null,
                        List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)))
                .markPaid(Instant.now(), "pi_123").refund();
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.create(order.getId(), "reason")).isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsAnOrderThatWasNotRefunded() {
        Order order = Order.create("buyer-1", null,
                        List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)))
                .markPaid(Instant.now(), "pi_123").markShipped(Instant.now());
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.create(order.getId(), "reason")).isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsWhenAReturnIsAlreadyRequestedForTheOrder() {
        Order order = shippedRefundedOrder();
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
        when(returnRequestRepositoryPort.findByOrderId(order.getId()))
                .thenReturn(List.of(ReturnRequest.create(order.getId(), "earlier reason")));

        assertThatThrownBy(() -> useCase.create(order.getId(), "reason")).isInstanceOf(ConflictException.class);
    }

    @Test
    void create_throwsForAnUnknownOrder() {
        when(orderRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.create("missing", "reason")).isInstanceOf(OrderNotFoundException.class);
    }
}
