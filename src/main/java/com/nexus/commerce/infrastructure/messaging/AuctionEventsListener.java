package com.nexus.commerce.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.commerce.application.usecase.CreateOrderFromAuctionUseCase;
import com.nexus.common.events.AuctionWonEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// The SRS requires that auction settlement hand off order creation to the Commerce Service
// instead of auction-service creating orders itself. This listener is that handoff: it only
// reacts to "AuctionWon" (an auction that ended with a winner); every other auction-events
// message is irrelevant here and is ignored.
@Component
public class AuctionEventsListener {

    private static final Logger log = LoggerFactory.getLogger(AuctionEventsListener.class);

    private final ObjectMapper objectMapper;
    private final CreateOrderFromAuctionUseCase createOrderFromAuctionUseCase;

    public AuctionEventsListener(ObjectMapper objectMapper,
                                  CreateOrderFromAuctionUseCase createOrderFromAuctionUseCase) {
        this.objectMapper = objectMapper;
        this.createOrderFromAuctionUseCase = createOrderFromAuctionUseCase;
    }

    @KafkaListener(topics = "auction-events")
    public void onMessage(String rawPayload) {
        try {
            String eventType = objectMapper.readTree(rawPayload).path("eventType").asText();
            if (!"AuctionWon".equals(eventType)) {
                return;
            }
            AuctionWonEvent event = objectMapper.readValue(rawPayload, AuctionWonEvent.class);
            createOrderFromAuctionUseCase.createFromAuction(event.getAuctionId(), event.getProductId(),
                    event.getSellerId(), event.getWinnerId(), event.getFinalPrice());
        } catch (Exception e) {
            log.error("Failed to process auction-events message: {}", rawPayload, e);
        }
    }
}
