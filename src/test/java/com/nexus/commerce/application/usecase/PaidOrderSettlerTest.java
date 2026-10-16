package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaidOrderSettlerTest {

    private OrderRepositoryPort orderRepositoryPort;
    private PaymentGatewayPort paymentGatewayPort;
    private EventPublisherPort eventPublisherPort;
    private PaidOrderSettler settler;

    private Order awaitingPaymentOrder() {
        return Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        paymentGatewayPort = mock(PaymentGatewayPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        settler = new PaidOrderSettler(orderRepositoryPort, paymentGatewayPort, eventPublisherPort);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void settle_leavesAdminRefundedOrderAlone() {
        Order order = awaitingPaymentOrder().markPaid(Instant.now(), "pi_test").refund();

        assertThat(settler.settle(order, "pi_test").getStatus()).isEqualTo(OrderStatus.REFUNDED);
        verifyNoInteractions(paymentGatewayPort, eventPublisherPort);
        verify(orderRepositoryPort, never()).save(any());
    }

    // If Stripe's refund fails nothing is saved: the exception rolls the transaction back and
    // Stripe redelivers the webhook later.
    @Test
    void settle_savesNothingWhenRefundFails() {
        Order order = awaitingPaymentOrder().cancel();
        doThrow(new RuntimeException("stripe down")).when(paymentGatewayPort).refund("pi_test");

        assertThatThrownBy(() -> settler.settle(order, "pi_test")).isInstanceOf(RuntimeException.class);
        verify(orderRepositoryPort, never()).save(any());
        verifyNoInteractions(eventPublisherPort);
    }
}
