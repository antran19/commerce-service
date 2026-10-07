package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
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
import static org.mockito.Mockito.*;

class ConfirmPaymentUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private PaymentGatewayPort paymentGatewayPort;
    private EventPublisherPort eventPublisherPort;
    private ConfirmPaymentUseCase useCase;

    private Order awaitingPaymentOrder() {
        return Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        paymentGatewayPort = mock(PaymentGatewayPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new ConfirmPaymentUseCase(orderRepositoryPort, paymentGatewayPort, eventPublisherPort);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void confirm_marksOrderPaidAndPublishesEvent() {
        Order order = awaitingPaymentOrder();
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(paymentGatewayPort.retrieveSessionStatus("sess-1"))
                .thenReturn(new PaymentGatewayPort.PaymentStatus(true, order.getId(), "buyer-1"));

        OrderResult result = useCase.confirm(order.getId(), "buyer-1", "sess-1");

        assertThat(result.status()).isEqualTo("PAID");
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void confirm_isIdempotentWhenAlreadyPaid() {
        Order order = awaitingPaymentOrder().markPaid(Instant.now());
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        OrderResult result = useCase.confirm(order.getId(), "buyer-1", "sess-1");

        assertThat(result.status()).isEqualTo("PAID");
        verify(paymentGatewayPort, never()).retrieveSessionStatus(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void confirm_rejectsWhenStripeHasNotConfirmed() {
        Order order = awaitingPaymentOrder();
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(paymentGatewayPort.retrieveSessionStatus("sess-1"))
                .thenReturn(new PaymentGatewayPort.PaymentStatus(false, order.getId(), "buyer-1"));

        assertThatThrownBy(() -> useCase.confirm(order.getId(), "buyer-1", "sess-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void confirm_rejectsSessionBelongingToAnotherOrder() {
        Order order = awaitingPaymentOrder();
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(paymentGatewayPort.retrieveSessionStatus("sess-1"))
                .thenReturn(new PaymentGatewayPort.PaymentStatus(true, "some-other-order", "buyer-1"));

        assertThatThrownBy(() -> useCase.confirm(order.getId(), "buyer-1", "sess-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void confirm_rejectsNonBuyerCaller() {
        Order order = awaitingPaymentOrder();
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.confirm(order.getId(), "someone-else", "sess-1"))
                .isInstanceOf(ForbiddenException.class);
    }
}
