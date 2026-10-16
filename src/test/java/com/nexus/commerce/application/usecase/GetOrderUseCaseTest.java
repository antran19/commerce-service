package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.ShippingAddress;
import com.nexus.common.core.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetOrderUseCaseTest {

    private OrderRepositoryPort orderRepositoryPort;
    private GetOrderUseCase useCase;
    private Order order;

    @BeforeEach
    void setUp() {
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        useCase = new GetOrderUseCase(orderRepositoryPort);
        order = Order.create("buyer-1", null,
                        List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)))
                .withShippingAddress(ShippingAddress.create("Nguyen Van A", "0901234567", "12 Le Loi", "HCM"));
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
    }

    @Test
    void getOrder_returnsTheOrderWithItsAddressToTheBuyer() {
        OrderResult result = useCase.getOrder(order.getId(), "buyer-1", false);

        assertThat(result.id()).isEqualTo(order.getId());
        assertThat(result.shippingAddress().phone()).isEqualTo("0901234567");
    }

    @Test
    void getOrder_allowsASellerWithAnItemInTheOrder() {
        assertThat(useCase.getOrder(order.getId(), "seller-1", false).id()).isEqualTo(order.getId());
    }

    @Test
    void getOrder_allowsStaff() {
        assertThat(useCase.getOrder(order.getId(), "admin-1", true).id()).isEqualTo(order.getId());
    }

    @Test
    void getOrder_hidesTheOrderAndItsAddressFromAStranger() {
        assertThatThrownBy(() -> useCase.getOrder(order.getId(), "stranger", false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getOrder_throwsWhenTheOrderDoesNotExist() {
        when(orderRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getOrder("missing", "buyer-1", false))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
