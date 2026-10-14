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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetOrderInvoiceUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private GetOrderInvoiceUseCase useCase;

    private static Order unpaidOrder() {
        return Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 2)));
    }

    private Order stored(Order order) {
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
        return order;
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        useCase = new GetOrderInvoiceUseCase(orderRepositoryPort);
    }

    @Test
    void getInvoice_returnsTheInvoiceForTheBuyerOfAPaidOrder() {
        Order order = stored(unpaidOrder().markPaid(Instant.parse("2026-10-14T03:00:00Z"), "pi_123"));

        InvoiceResult invoice = useCase.getInvoice(order.getId(), "buyer-1", false);

        assertThat(invoice.orderId()).isEqualTo(order.getId());
        assertThat(invoice.buyerId()).isEqualTo("buyer-1");
        assertThat(invoice.totalAmount()).isEqualByComparingTo("200.00");
        assertThat(invoice.items()).hasSize(1);
        assertThat(invoice.orderStatus()).isEqualTo("PAID");
        assertThat(invoice.issuedAt()).isEqualTo(Instant.parse("2026-10-14T03:00:00Z"));
    }

    @Test
    void getInvoice_numberIsYearPaidPlusFirstEightCharsOfTheOrderId() {
        Order order = stored(unpaidOrder().markPaid(Instant.parse("2026-10-14T03:00:00Z"), "pi_123"));
        String expectedSuffix = order.getId().replace("-", "").substring(0, 8).toUpperCase();

        InvoiceResult invoice = useCase.getInvoice(order.getId(), "buyer-1", false);

        assertThat(invoice.invoiceNumber()).isEqualTo("INV-2026-" + expectedSuffix);
    }

    @Test
    void getInvoice_allowsASellerWhoHasAnItemInTheOrder() {
        Order order = stored(unpaidOrder().markPaid(Instant.now(), "pi_123"));

        assertThat(useCase.getInvoice(order.getId(), "seller-1", false).orderId()).isEqualTo(order.getId());
    }

    @Test
    void getInvoice_allowsStaffEvenIfTheyAreNotInTheOrder() {
        Order order = stored(unpaidOrder().markPaid(Instant.now(), "pi_123"));

        assertThat(useCase.getInvoice(order.getId(), "admin-1", true).orderId()).isEqualTo(order.getId());
    }

    @Test
    void getInvoice_rejectsSomeoneWhoIsNotInTheOrder() {
        Order order = stored(unpaidOrder().markPaid(Instant.now(), "pi_123"));

        assertThatThrownBy(() -> useCase.getInvoice(order.getId(), "stranger", false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getInvoice_rejectsAnOrderThatHasNotBeenPaid() {
        Order order = stored(unpaidOrder());

        assertThatThrownBy(() -> useCase.getInvoice(order.getId(), "buyer-1", false))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getInvoice_checksAccessBeforeRevealingWhetherTheOrderIsPaid() {
        Order order = stored(unpaidOrder());

        assertThatThrownBy(() -> useCase.getInvoice(order.getId(), "stranger", false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getInvoice_stillIssuesTheInvoiceForARefundedOrder() {
        Order order = stored(unpaidOrder().markPaid(Instant.now(), "pi_123").refund());

        InvoiceResult invoice = useCase.getInvoice(order.getId(), "buyer-1", false);

        assertThat(invoice.orderStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void getInvoice_throwsWhenTheOrderDoesNotExist() {
        when(orderRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getInvoice("missing", "buyer-1", false))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
