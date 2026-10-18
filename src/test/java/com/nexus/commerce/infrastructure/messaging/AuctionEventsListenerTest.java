package com.nexus.commerce.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.commerce.application.usecase.CancelDefaultedAuctionOrderUseCase;
import com.nexus.commerce.application.usecase.CreateOrderFromAuctionUseCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuctionEventsListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CreateOrderFromAuctionUseCase createOrderFromAuctionUseCase = mock(CreateOrderFromAuctionUseCase.class);
    private final CancelDefaultedAuctionOrderUseCase cancelDefaultedAuctionOrderUseCase =
            mock(CancelDefaultedAuctionOrderUseCase.class);
    private final AuctionEventsListener listener = new AuctionEventsListener(objectMapper, createOrderFromAuctionUseCase,
            cancelDefaultedAuctionOrderUseCase);

    @Test
    void auctionWon_createsTheWinnersOrder() throws Exception {
        listener.onMessage("{\"eventId\":\"e\",\"eventType\":\"AuctionWon\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"auction-1\",\"auctionId\":\"auction-1\",\"productId\":\"product-1\",\"sellerId\":\"s\","
                + "\"winnerId\":\"w\",\"finalPrice\":150}");

        verify(createOrderFromAuctionUseCase).createFromAuction(eq("auction-1"), eq("product-1"), eq("s"), eq("w"),
                argThat(p -> p.compareTo(new BigDecimal("150")) == 0));
    }

    @Test
    void auctionPaymentTimeout_cancelsTheWinnersUnpaidOrder() throws Exception {
        listener.onMessage("{\"eventId\":\"e\",\"eventType\":\"AuctionPaymentTimeout\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"auction-1\",\"auctionId\":\"auction-1\",\"winnerId\":\"w\",\"productId\":\"product-1\"}");

        verify(cancelDefaultedAuctionOrderUseCase).cancel("auction-1");
        verifyNoInteractions(createOrderFromAuctionUseCase);
    }
}
