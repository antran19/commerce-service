package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.events.OrderPaidEvent;
import com.nexus.common.events.OrderRefundedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HandlePaymentWebhookUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private PaymentGatewayPort paymentGatewayPort;
    private EventPublisherPort eventPublisherPort;
    private HandlePaymentWebhookUseCase useCase;

    private Order awaitingPaymentOrder() {
        return Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
    }

    private void stripeSays(Order order, boolean paid, String buyerId) {
        when(paymentGatewayPort.verifyWebhookAndExtractSessionId("body", "sig")).thenReturn(Optional.of("sess-1"));
        when(paymentGatewayPort.retrieveSessionStatus("sess-1"))
                .thenReturn(new PaymentGatewayPort.PaymentStatus(paid, order.getId(), buyerId, "pi_test"));
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        paymentGatewayPort = mock(PaymentGatewayPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new HandlePaymentWebhookUseCase(orderRepositoryPort, paymentGatewayPort,
                new PaidOrderSettler(orderRepositoryPort, paymentGatewayPort, eventPublisherPort));
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void handle_marksAwaitingOrderPaid_whenBuyerNeverCameBack() {
        Order order = awaitingPaymentOrder();
        stripeSays(order, true, "buyer-1");

        useCase.handle("body", "sig");

        verify(orderRepositoryPort).save(argThat(o -> o.getStatus() == OrderStatus.PAID
                && "pi_test".equals(o.getPaymentReference())));
        verify(eventPublisherPort).publish(any(OrderPaidEvent.class));
    }

    @Test
    void handle_isNoOpForRedeliveredEvent() {
        Order order = awaitingPaymentOrder().markPaid(Instant.now(), "pi_test");
        stripeSays(order, true, "buyer-1");

        useCase.handle("body", "sig");

        verify(orderRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void handle_refundsPaymentForCancelledOrder() {
        Order order = awaitingPaymentOrder().cancel();
        stripeSays(order, true, "buyer-1");

        useCase.handle("body", "sig");

        verify(paymentGatewayPort).refund("pi_test");
        verify(orderRepositoryPort).save(argThat(o -> o.getStatus() == OrderStatus.REFUNDED));
        verify(eventPublisherPort).publish(argThat(e -> e instanceof OrderRefundedEvent r
                && PaidOrderSettler.LATE_PAYMENT_REASON.equals(r.getReason())));
    }

    @Test
    void handle_ignoresOtherEventTypes() {
        when(paymentGatewayPort.verifyWebhookAndExtractSessionId("body", "sig")).thenReturn(Optional.empty());

        useCase.handle("body", "sig");

        verify(paymentGatewayPort, never()).retrieveSessionStatus(any());
        verifyNoInteractions(orderRepositoryPort, eventPublisherPort);
    }

    @Test
    void handle_ignoresSessionNotPaidYet() {
        Order order = awaitingPaymentOrder();
        stripeSays(order, false, "buyer-1");

        useCase.handle("body", "sig");

        verify(orderRepositoryPort, never()).save(any());
        verifyNoInteractions(eventPublisherPort);
    }

    @Test
    void handle_ignoresUnknownOrder() {
        when(paymentGatewayPort.verifyWebhookAndExtractSessionId("body", "sig")).thenReturn(Optional.of("sess-1"));
        when(paymentGatewayPort.retrieveSessionStatus("sess-1"))
                .thenReturn(new PaymentGatewayPort.PaymentStatus(true, "not-ours", "buyer-1", "pi_test"));
        when(orderRepositoryPort.findByIdForUpdate("not-ours")).thenReturn(Optional.empty());

        useCase.handle("body", "sig");

        verify(orderRepositoryPort, never()).save(any());
        verify(paymentGatewayPort, never()).refund(any());
    }

    @Test
    void handle_ignoresBuyerMismatch() {
        Order order = awaitingPaymentOrder();
        stripeSays(order, true, "someone-else");

        useCase.handle("body", "sig");

        verify(orderRepositoryPort, never()).save(any());
        verifyNoInteractions(eventPublisherPort);
    }
}
