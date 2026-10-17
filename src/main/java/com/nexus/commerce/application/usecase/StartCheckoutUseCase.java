package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.application.port.out.ProductStockRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.ProductSnapshot;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.events.InventoryReservationRequestedEvent;
import com.nexus.common.events.OrderCreatedEvent;
import com.nexus.common.events.ReservationLineItem;

import java.util.List;

public class StartCheckoutUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;
    private final ProductSnapshotRepositoryPort productSnapshotRepositoryPort;
    private final ProductStockRepositoryPort productStockRepositoryPort;

    public StartCheckoutUseCase(CartRepositoryPort cartRepositoryPort, OrderRepositoryPort orderRepositoryPort,
                                 EventPublisherPort eventPublisherPort,
                                 ProductSnapshotRepositoryPort productSnapshotRepositoryPort,
                                 ProductStockRepositoryPort productStockRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.productSnapshotRepositoryPort = productSnapshotRepositoryPort;
        this.productStockRepositoryPort = productStockRepositoryPort;
    }

    public OrderResult checkout(String buyerId) {
        Cart cart = cartRepositoryPort.findByBuyerId(buyerId).orElseGet(() -> Cart.create(buyerId));
        if (cart.isEmpty()) {
            throw new ConflictException("CART_EMPTY", "Cannot checkout an empty cart");
        }

        List<OrderItem> items = cart.getItems().stream().map(this::repriced).map(OrderItem::fromCartItem).toList();
        Order order = Order.create(buyerId, null, items);
        Order saved = orderRepositoryPort.save(order);
        cartRepositoryPort.save(cart.clear());

        eventPublisherPort.publish(new OrderCreatedEvent(saved.getId(), null, null,
                saved.singleSellerIdOrNull(), saved.getBuyerId(), saved.getTotalAmount()));
        eventPublisherPort.publish(new InventoryReservationRequestedEvent(saved.getId(), toLineItems(items)));
        return OrderResult.from(saved);
    }

    // Re-reads price, name and seller from the catalog copy: the product may have been taken off
    // sale since it was added, and carts filled before prices were checked server-side may still
    // hold a price the client made up; stock may also have dropped below the cart quantity.
    // Throws before anything is saved, so the cart is kept.
    private CartItem repriced(CartItem item) {
        CartItem current = productSnapshotRepositoryPort.findById(item.getProductId())
                .filter(ProductSnapshot::isPurchasable)
                .map(p -> p.toCartItem(item.getQuantity()))
                .orElseThrow(() -> new ConflictException("CART_ITEM_UNAVAILABLE",
                        "Product in the cart is no longer available: " + item.getProductId()));
        StockGuard.requireAvailable(productStockRepositoryPort, item.getProductId(), item.getQuantity());
        return current;
    }

    // Stock is keyed by the PRODUCT id: this productId is sent in the "skuId" field of the event
    // and fulfillment-service stores it as sku_id, but it is NOT catalog's own Sku id (that is a
    // different, random value). Every product currently has exactly one SKU, so the two never
    // need to be told apart; if a product ever gets several SKUs, this needs the real SKU id.
    private static List<ReservationLineItem> toLineItems(List<OrderItem> items) {
        return items.stream().map(i -> new ReservationLineItem(i.getProductId(), i.getQuantity())).toList();
    }
}
