package com.nexus.commerce.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.commerce.application.usecase.CancelOrderDueToStockFailureUseCase;
import com.nexus.common.events.InventoryReservationFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// The other side of fulfillment-service's CommerceEventsListener: this is how
// commerce-service learns -- asynchronously, some time after the order was already created
// -- that stock couldn't be held for it. InventoryReserved is intentionally ignored here;
// a successful reservation doesn't need to change anything about the order's visible state.
@Component
public class FulfillmentEventsListener {


    private final ObjectMapper objectMapper;
    private final CancelOrderDueToStockFailureUseCase cancelOrderDueToStockFailureUseCase;

    public FulfillmentEventsListener(ObjectMapper objectMapper,
                                      CancelOrderDueToStockFailureUseCase cancelOrderDueToStockFailureUseCase) {
        this.objectMapper = objectMapper;
        this.cancelOrderDueToStockFailureUseCase = cancelOrderDueToStockFailureUseCase;
    }

    // Exceptions are NOT caught here on purpose: KafkaErrorHandlingConfig retries them, then
    // moves the record to the dead-letter topic instead of losing it.
    @KafkaListener(topics = "fulfillment-events")
    public void onMessage(String rawPayload) throws JsonProcessingException {
        String eventType = objectMapper.readTree(rawPayload).path("eventType").asText();
        if (!"InventoryReservationFailed".equals(eventType)) {
            return;
        }
        InventoryReservationFailedEvent event =
                objectMapper.readValue(rawPayload, InventoryReservationFailedEvent.class);
        cancelOrderDueToStockFailureUseCase.cancel(event.getOrderId(), event.getReason());
    }
}
