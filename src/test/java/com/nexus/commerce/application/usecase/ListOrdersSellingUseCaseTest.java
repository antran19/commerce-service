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

class ListOrdersSellingUseCaseTest {

    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final ListOrdersSellingUseCase useCase = new ListOrdersSellingUseCase(orderRepositoryPort);

    @Test
    void listForSeller_returnsOrdersContainingThatSellersItems() {
        Order order = Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
        when(orderRepositoryPort.findBySellerId("seller-1")).thenReturn(List.of(order));

        List<OrderResult> results = useCase.listForSeller("seller-1");

        assertThat(results).extracting(OrderResult::id).containsExactly(order.getId());
    }
}
