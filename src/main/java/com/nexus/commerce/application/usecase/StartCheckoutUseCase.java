package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.events.InventoryReservationRequestedEvent;
import com.nexus.common.events.OrderCreatedEvent;
import com.nexus.common.events.ReservationLineItem;

import java.util.List;

public class StartCheckoutUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public StartCheckoutUseCase(CartRepositoryPort cartRepositoryPort, OrderRepositoryPort orderRepositoryPort,
                                 EventPublisherPort eventPublisherPort) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    public OrderResult checkout(String buyerId) {
        Cart cart = cartRepositoryPort.findByBuyerId(buyerId).orElseGet(() -> Cart.create(buyerId));
        if (cart.isEmpty()) {
            throw new ConflictException("CART_EMPTY", "Cannot checkout an empty cart");
        }

        List<OrderItem> items = cart.getItems().stream().map(OrderItem::fromCartItem).toList();
        Order order = Order.create(buyerId, null, items);
        Order saved = orderRepositoryPort.save(order);
        cartRepositoryPort.save(cart.clear());

        eventPublisherPort.publish(new OrderCreatedEvent(saved.getId(), null, null,
                saved.singleSellerIdOrNull(), saved.getBuyerId(), saved.getTotalAmount()));
        eventPublisherPort.publish(new InventoryReservationRequestedEvent(saved.getId(), toLineItems(items)));
        return OrderResult.from(saved);
    }

    // productId doubles as fulfillment-service's skuId: every product currently has exactly
    // one SKU (see catalog-service's Sku model), so there is no real distinction yet. If that
    // 1:1 assumption ever changes, this needs the order item's actual SKU id instead.
    private static List<ReservationLineItem> toLineItems(List<OrderItem> items) {
        return items.stream().map(i -> new ReservationLineItem(i.getProductId(), i.getQuantity())).toList();
    }
}
