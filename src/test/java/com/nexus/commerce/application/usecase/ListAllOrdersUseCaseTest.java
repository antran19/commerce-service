package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListAllOrdersUseCaseTest {

    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final ListAllOrdersUseCase useCase = new ListAllOrdersUseCase(orderRepositoryPort);

    @Test
    void list_returnsEveryOrderRegardlessOfBuyer() {
        Order order1 = Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
        Order order2 = Order.create("buyer-2", null,
                List.of(new OrderItem("product-2", "seller-2", "Mouse Y", new BigDecimal("20.00"), 1)));
        when(orderRepositoryPort.findAll()).thenReturn(List.of(order1, order2));

        List<OrderResult> results = useCase.list();

        assertThat(results).extracting(OrderResult::buyerId).containsExactly("buyer-1", "buyer-2");
    }
}
