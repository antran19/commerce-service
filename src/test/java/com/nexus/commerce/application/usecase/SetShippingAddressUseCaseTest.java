package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.core.exception.ValidationException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetShippingAddressUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private SetShippingAddressUseCase useCase;

    private static Order unpaidOrder() {
        return Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
    }

    private Order stored(Order order) {
        when(orderRepositoryPort.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        return order;
    }

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        useCase = new SetShippingAddressUseCase(orderRepositoryPort);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void set_savesTheAddressOnAnUnpaidOrderOfTheCaller() {
        Order order = stored(unpaidOrder());

        OrderResult result = useCase.setShippingAddress(order.getId(), "buyer-1", "Nguyen Van A", "0901234567",
                "12 Le Loi", "HCM");

        assertThat(result.shippingAddress().recipientName()).isEqualTo("Nguyen Van A");
        assertThat(result.shippingAddress().city()).isEqualTo("HCM");
        verify(orderRepositoryPort).save(any(Order.class));
    }

    @Test
    void set_worksForAnAuctionOrder() {
        Order order = stored(Order.create("buyer-1", "auction-1",
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1))));

        OrderResult result = useCase.setShippingAddress(order.getId(), "buyer-1", "A", "0901234567", "1 A", "HCM");

        assertThat(result.shippingAddress()).isNotNull();
    }

    @Test
    void set_rejectsSomeoneWhoIsNotTheBuyer() {
        Order order = stored(unpaidOrder());

        assertThatThrownBy(() -> useCase.setShippingAddress(order.getId(), "someone-else", "A", "0901234567",
                "1 A", "HCM")).isInstanceOf(ForbiddenException.class);
        verify(orderRepositoryPort, never()).save(any(Order.class));
    }

    @Test
    void set_rejectsAnOrderThatIsNoLongerAwaitingPayment() {
        Order paid = stored(unpaidOrder().markPaid(Instant.now(), "pi_1"));

        assertThatThrownBy(() -> useCase.setShippingAddress(paid.getId(), "buyer-1", "A", "0901234567", "1 A", "HCM"))
                .isInstanceOf(ConflictException.class);
        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepositoryPort, never()).save(any(Order.class));
    }

    @Test
    void set_rejectsAnInvalidAddressWithoutSaving() {
        Order order = stored(unpaidOrder());

        assertThatThrownBy(() -> useCase.setShippingAddress(order.getId(), "buyer-1", "A", "123", "1 A", "HCM"))
                .isInstanceOf(ValidationException.class);
        verify(orderRepositoryPort, never()).save(any(Order.class));
    }

    @Test
    void set_throwsWhenTheOrderDoesNotExist() {
        when(orderRepositoryPort.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.setShippingAddress("missing", "buyer-1", "A", "0901234567", "1 A", "HCM"))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
