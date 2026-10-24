package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.commerce.domain.model.ShippingAddress;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import com.nexus.common.core.exception.ValidationException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateCheckoutSessionUseCaseTest {

    private static final String OK_URL = "http://localhost:5173/payment/success";
    private static final String CANCEL_URL = "http://localhost:5173/payment/cancel";

    private OrderRepositoryPort orderRepositoryPort;
    private PaymentGatewayPort paymentGatewayPort;
    private CreateCheckoutSessionUseCase useCase;

    private Order order(String buyerId, OrderStatus status) {
        Order base = Order.create(buyerId, null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
        return Order.reconstitute(base.getId(), buyerId, null, base.getItems(), base.getTotalAmount(),
                status, base.getCreatedAt(), status == OrderStatus.PAID ? Instant.now() : null,
                status == OrderStatus.PAID ? "pi_test" : null, null,
                ShippingAddress.create("Nguyen Van A", "0901234567", "12 Le Loi", "Ho Chi Minh"));
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        paymentGatewayPort = mock(PaymentGatewayPort.class);
        useCase = new CreateCheckoutSessionUseCase(orderRepositoryPort, paymentGatewayPort,
                new RedirectUrlPolicy(java.util.List.of("http://localhost:5173")));
    }

    @Test
    void createSession_returnsCheckoutUrlForTheBuyer() {
        Order order = order("buyer-1", OrderStatus.AWAITING_PAYMENT);
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentGatewayPort.createCheckoutSession(eq(order.getId()), eq("buyer-1"), any(), any(), any()))
                .thenReturn(new PaymentGatewayPort.CheckoutSession("sess-1", "https://checkout.stripe.com/sess-1"));

        PaymentGatewayPort.CheckoutSession session = useCase.createSession(order.getId(), "buyer-1",
                OK_URL, CANCEL_URL);

        assertThat(session.checkoutUrl()).isEqualTo("https://checkout.stripe.com/sess-1");
    }

    @Test
    void createSession_requiresAShippingAddress() {
        Order base = order("buyer-1", OrderStatus.AWAITING_PAYMENT);
        Order noAddress = Order.reconstitute(base.getId(), "buyer-1", null, base.getItems(), base.getTotalAmount(),
                OrderStatus.AWAITING_PAYMENT, base.getCreatedAt(), null, null, null);
        when(orderRepositoryPort.findById(noAddress.getId())).thenReturn(Optional.of(noAddress));

        assertThatThrownBy(() -> useCase.createSession(noAddress.getId(), "buyer-1", OK_URL, CANCEL_URL))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("shipping address");
    }

    @Test
    void createSession_rejectsNonBuyerCaller() {
        Order order = order("buyer-1", OrderStatus.AWAITING_PAYMENT);
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.createSession(order.getId(), "someone-else", OK_URL, CANCEL_URL))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createSession_rejectsAlreadyPaidOrder() {
        Order order = order("buyer-1", OrderStatus.PAID);
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.createSession(order.getId(), "buyer-1", OK_URL, CANCEL_URL))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createSession_rejectsUnknownOrder() {
        when(orderRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createSession("missing", "buyer-1", OK_URL, CANCEL_URL))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // The addresses go to Stripe and decide where the buyer lands after paying: a foreign one must
    // never reach the payment gateway, whoever the buyer is.
    @Test
    void createSession_rejectsARedirectToAnotherSite_andNeverCallsTheGateway() {
        Order order = order("buyer-1", OrderStatus.AWAITING_PAYMENT);
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.createSession(order.getId(), "buyer-1", "https://evil.example.org/phish", CANCEL_URL))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.createSession(order.getId(), "buyer-1", OK_URL, "http://localhost:5173@evil.com/"))
                .isInstanceOf(ValidationException.class);

        verify(paymentGatewayPort, never()).createCheckoutSession(anyString(), anyString(), any(), anyString(), anyString());
    }
}
