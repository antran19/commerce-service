package com.nexus.commerce.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.commerce.application.usecase.SyncProductSnapshotUseCase;
import com.nexus.common.events.ProductCreatedEvent;
import com.nexus.common.events.ProductStatusChangedEvent;
import com.nexus.common.events.ProductUpdatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Feeds commerce-service's copy of catalog products (price, name, seller, status), which is what
// carts and checkout price from. Category events on the same topic are ignored.
@Component
public class CatalogEventsListener {

    private final ObjectMapper objectMapper;
    private final SyncProductSnapshotUseCase syncProductSnapshotUseCase;

    public CatalogEventsListener(ObjectMapper objectMapper, SyncProductSnapshotUseCase syncProductSnapshotUseCase) {
        this.objectMapper = objectMapper;
        this.syncProductSnapshotUseCase = syncProductSnapshotUseCase;
    }

    // Exceptions are NOT caught here on purpose: NexusKafkaAutoConfiguration (common-kafka) retries them, then
    // moves the record to the dead-letter topic instead of losing it.
    @KafkaListener(topics = "catalog-events")
    public void onMessage(String rawPayload) throws JsonProcessingException {
        String eventType = objectMapper.readTree(rawPayload).path("eventType").asText();
        switch (eventType) {
            case "ProductCreated" -> {
                ProductCreatedEvent e = objectMapper.readValue(rawPayload, ProductCreatedEvent.class);
                syncProductSnapshotUseCase.onCreated(e.getProductId(), e.getSellerId(), e.getName(), e.getPrice());
            }
            case "ProductStatusChanged" -> {
                ProductStatusChangedEvent e = objectMapper.readValue(rawPayload, ProductStatusChangedEvent.class);
                syncProductSnapshotUseCase.onStatusChanged(e.getProductId(), e.getNewStatus());
            }
            case "ProductUpdated" -> {
                ProductUpdatedEvent e = objectMapper.readValue(rawPayload, ProductUpdatedEvent.class);
                syncProductSnapshotUseCase.onRenamed(e.getProductId(), e.getName());
            }
            default -> {
                // Category events and anything newer: nothing commerce needs.
            }
        }
    }
}
