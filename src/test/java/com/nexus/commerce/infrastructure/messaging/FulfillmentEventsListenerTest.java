package com.nexus.commerce.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.commerce.application.usecase.CancelOrderDueToStockFailureUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FulfillmentEventsListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CancelOrderDueToStockFailureUseCase cancelOrderDueToStockFailureUseCase =
            mock(CancelOrderDueToStockFailureUseCase.class);
    private final FulfillmentEventsListener listener =
            new FulfillmentEventsListener(objectMapper, cancelOrderDueToStockFailureUseCase);

    @Test
    void onMessage_reservationFailed_cancelsTheOrder() {
        String payload = "{\"eventId\":\"e-1\",\"eventType\":\"InventoryReservationFailed\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"aggregateId\":\"order-1\",\"orderId\":\"order-1\","
                + "\"reason\":\"Not enough stock\"}";

        listener.onMessage(payload);

        verify(cancelOrderDueToStockFailureUseCase).cancel("order-1", "Not enough stock");
    }

    @Test
    void onMessage_reserved_doesNothing() {
        String payload = "{\"eventId\":\"e-2\",\"eventType\":\"InventoryReserved\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"aggregateId\":\"order-2\",\"orderId\":\"order-2\"}";

        listener.onMessage(payload);

        verify(cancelOrderDueToStockFailureUseCase, never()).cancel(any(), any());
    }

    @Test
    void onMessage_malformedPayload_doesNotThrow() {
        assertThatCode(() -> listener.onMessage("{not valid json")).doesNotThrowAnyException();
    }
}
