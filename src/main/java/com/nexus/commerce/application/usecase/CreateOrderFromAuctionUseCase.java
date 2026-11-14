package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.ProductSnapshot;
import com.nexus.common.events.OrderCreatedEvent;

import java.math.BigDecimal;
import java.util.List;

// Triggered by consuming AuctionWonEvent off the auction-events Kafka topic. The SRS requires
// that "the auction service shall not create orders directly" -- this use case is the Commerce
// Service side of that handoff.
public class CreateOrderFromAuctionUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    private final ProductSnapshotRepositoryPort productSnapshotRepositoryPort;

    public CreateOrderFromAuctionUseCase(OrderRepositoryPort orderRepositoryPort,
                                          EventPublisherPort eventPublisherPort,
                                          ProductSnapshotRepositoryPort productSnapshotRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.productSnapshotRepositoryPort = productSnapshotRepositoryPort;
    }

    // Idempotent: Kafka delivers at-least-once, so a redelivered AuctionWonEvent for an auction
    // that already has an order must be a no-op, not a duplicate order.
    public OrderResult createFromAuction(String auctionId, String productId, String sellerId,
                                          String winnerId, BigDecimal finalPrice) {
        var existing = orderRepositoryPort.findByAuctionId(auctionId);
        if (existing.isPresent()) {
            return OrderResult.from(existing.get());
        }

        // AuctionWonEvent does not carry the product's display name, and there is no synchronous
        // service-to-service call in this codebase's conventions. This service already keeps a copy of
        // the catalog (fed by catalog events, the one checkout uses), so the name comes from there; it
        // is what the buyer reads on the order page and on the Stripe payment page. A product the copy
        // does not know (its event not consumed yet) falls back to a generic label.
        OrderItem item = new OrderItem(productId, sellerId, itemName(productId), finalPrice, 1);
        Order order = Order.create(winnerId, auctionId, List.of(item));
        Order saved = orderRepositoryPort.save(order);

        // No InventoryReservationRequested here, unlike checkout: fulfillment has held the unit
        // since the auction was created, and commits that hold when this order is paid. Asking
        // again would hold a second unit -- or fail, the only one being held -- and cancel the
        // winner's order.
        eventPublisherPort.publish(new OrderCreatedEvent(saved.getId(), auctionId, productId, sellerId,
                winnerId, saved.getTotalAmount()));
        return OrderResult.from(saved);
    }

    private String itemName(String productId) {
        return productSnapshotRepositoryPort.findById(productId)
                .map(ProductSnapshot::getName)
                .filter(name -> !name.isBlank())
                .orElse("Auction item " + productId);
    }
}
