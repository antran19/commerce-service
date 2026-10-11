package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.exception.ReturnRequestNotFoundException;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.ReturnRequest;
import com.nexus.common.events.OrderReturnReceivedEvent;
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

class ReceiveReturnUseCaseTest {

    private ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private OrderRepositoryPort orderRepositoryPort;
    private EventPublisherPort eventPublisherPort;
    private ReceiveReturnUseCase useCase;

    private Order order() {
        return Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 2)));
    }

    @BeforeEach
    void setUp() {
        returnRequestRepositoryPort = mock(ReturnRequestRepositoryPort.class);
        orderRepositoryPort = mock(OrderRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new ReceiveReturnUseCase(returnRequestRepositoryPort, orderRepositoryPort, eventPublisherPort);
        when(returnRequestRepositoryPort.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void receive_marksReceivedAndPublishesRestockEvent() {
        Order order = order();
        ReturnRequest returnRequest = ReturnRequest.create(order.getId(), "Wrong item");
        when(returnRequestRepositoryPort.findByIdForUpdate(returnRequest.getId())).thenReturn(Optional.of(returnRequest));
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        ReturnRequestResult result = useCase.receive(returnRequest.getId(), "admin-1");

        assertThat(result.status()).isEqualTo("RECEIVED");
        verify(eventPublisherPort).publish(argThat(e -> e instanceof OrderReturnReceivedEvent evt
                && evt.getOrderId().equals(order.getId()) && evt.getItems().size() == 1
                && evt.getItems().get(0).skuId().equals("product-1") && evt.getItems().get(0).quantity() == 2));
    }

    @Test
    void receive_isIdempotentWhenAlreadyReceived() {
        Order order = order();
        ReturnRequest returnRequest = ReturnRequest.create(order.getId(), "Wrong item").receive("admin-1");
        when(returnRequestRepositoryPort.findByIdForUpdate(returnRequest.getId())).thenReturn(Optional.of(returnRequest));

        ReturnRequestResult result = useCase.receive(returnRequest.getId(), "admin-2");

        assertThat(result.status()).isEqualTo("RECEIVED");
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void receive_throwsForAnUnknownReturnRequest() {
        when(returnRequestRepositoryPort.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.receive("missing", "admin-1"))
                .isInstanceOf(ReturnRequestNotFoundException.class);
    }

    @Test
    void receive_throwsWhenTheOrderCannotBeFound() {
        ReturnRequest returnRequest = ReturnRequest.create("order-missing", "Wrong item");
        when(returnRequestRepositoryPort.findByIdForUpdate(returnRequest.getId())).thenReturn(Optional.of(returnRequest));
        when(orderRepositoryPort.findById("order-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.receive(returnRequest.getId(), "admin-1"))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
