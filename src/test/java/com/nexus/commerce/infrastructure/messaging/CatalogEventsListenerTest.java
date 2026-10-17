package com.nexus.commerce.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.commerce.application.usecase.SyncProductSnapshotUseCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CatalogEventsListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SyncProductSnapshotUseCase syncProductSnapshotUseCase = mock(SyncProductSnapshotUseCase.class);
    private final CatalogEventsListener listener = new CatalogEventsListener(objectMapper, syncProductSnapshotUseCase);

    @Test
    void onMessage_productCreated_storesTheCatalogCopy() throws Exception {
        String payload = "{\"eventId\":\"e-1\",\"eventType\":\"ProductCreated\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"product-1\",\"productId\":\"product-1\",\"name\":\"Laptop X\",\"categoryId\":\"cat-1\","
                + "\"sellerId\":\"seller-1\",\"initialStock\":5,\"price\":999.00}";

        listener.onMessage(payload);

        verify(syncProductSnapshotUseCase).onCreated(eq("product-1"), eq("seller-1"), eq("Laptop X"),
                argThat(p -> p.compareTo(new BigDecimal("999.00")) == 0));
    }

    @Test
    void onMessage_productStatusChanged_updatesTheStatus() throws Exception {
        String payload = "{\"eventId\":\"e-2\",\"eventType\":\"ProductStatusChanged\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"product-1\",\"productId\":\"product-1\",\"oldStatus\":\"DRAFT\",\"newStatus\":\"ACTIVE\"}";

        listener.onMessage(payload);

        verify(syncProductSnapshotUseCase).onStatusChanged("product-1", "ACTIVE");
    }

    @Test
    void onMessage_productUpdated_updatesTheName() throws Exception {
        String payload = "{\"eventId\":\"e-3\",\"eventType\":\"ProductUpdated\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"product-1\",\"productId\":\"product-1\",\"name\":\"Laptop X Pro\"}";

        listener.onMessage(payload);

        verify(syncProductSnapshotUseCase).onRenamed("product-1", "Laptop X Pro");
    }

    @Test
    void onMessage_otherCatalogEvents_areIgnored() throws Exception {
        String payload = "{\"eventId\":\"e-4\",\"eventType\":\"CategoryCreated\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"cat-1\",\"categoryId\":\"cat-1\",\"name\":\"Watches\"}";

        listener.onMessage(payload);

        verifyNoInteractions(syncProductSnapshotUseCase);
    }

    @Test
    void onMessage_malformedPayload_throwsSoItGoesToDeadLetterTopic() {
        assertThatThrownBy(() -> listener.onMessage("{not valid json"))
                .isInstanceOf(JsonProcessingException.class);
        verify(syncProductSnapshotUseCase, org.mockito.Mockito.never()).onCreated(any(), any(), any(), any());
    }
}
