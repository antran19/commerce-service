package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.events.OrderRefundedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefundOrderUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private PaymentGatewayPort paymentGatewayPort;
    private EventPublisherPort eventPublisherPort;
    private RefundOrderUseCase useCase;

    private Order paidOrder() {
        Order order = Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
        return order.markPaid(Instant.now(), "pi_123");
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        paymentGatewayPort = mock(PaymentGatewayPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new RefundOrderUseCase(orderRepositoryPort, paymentGatewayPort, eventPublisherPort);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void refund_refundsViaGatewayAndMarksOrderRefunded() {
        Order order = paidOrder();
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        OrderResult result = useCase.refund(order.getId(), "Customer requested refund");

        assertThat(result.status()).isEqualTo("REFUNDED");
        verify(paymentGatewayPort).refund("pi_123");
        verify(eventPublisherPort).publish(argThat(e -> e instanceof OrderRefundedEvent evt
                && evt.getOrderId().equals(order.getId()) && evt.getReason().equals("Customer requested refund")));
    }

    @Test
    void refund_rejectsAnOrderThatIsNotPaid() {
        Order order = Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.refund(order.getId(), "reason"))
                .isInstanceOf(ConflictException.class);

        verify(paymentGatewayPort, never()).refund(any());
    }

    @Test
    void refund_isIdempotentWhenAlreadyRefunded() {
        Order order = paidOrder().refund();
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        OrderResult result = useCase.refund(order.getId(), "reason");

        assertThat(result.status()).isEqualTo("REFUNDED");
        verify(paymentGatewayPort, never()).refund(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void refund_throwsForAnUnknownOrder() {
        when(orderRepositoryPort.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.refund("missing", "reason")).isInstanceOf(OrderNotFoundException.class);
    }
}
