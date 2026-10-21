package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.CheckoutRequestRepositoryPort;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.application.port.out.ProductStockRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.ProductSnapshot;
import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ValidationException;
import com.nexus.common.events.InventoryReservationRequestedEvent;
import com.nexus.common.events.OrderCreatedEvent;
import com.nexus.common.events.ReservationLineItem;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// One transaction around the whole checkout: the order, the emptied cart, the idempotency record
// and the two outbox events are saved together or not at all. Before this, a crash between the
// steps could leave an order with no reservation event (stuck AWAITING_PAYMENT for ever).
@Transactional
public class StartCheckoutUseCase {

    // Longest Idempotency-Key we accept (the checkout_requests column is VARCHAR(100)).
    static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final CartRepositoryPort cartRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;
    private final ProductSnapshotRepositoryPort productSnapshotRepositoryPort;
    private final ProductStockRepositoryPort productStockRepositoryPort;
    private final CheckoutRequestRepositoryPort checkoutRequestRepositoryPort;

    public StartCheckoutUseCase(CartRepositoryPort cartRepositoryPort, OrderRepositoryPort orderRepositoryPort,
                                 EventPublisherPort eventPublisherPort,
                                 ProductSnapshotRepositoryPort productSnapshotRepositoryPort,
                                 ProductStockRepositoryPort productStockRepositoryPort,
                                 CheckoutRequestRepositoryPort checkoutRequestRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.productSnapshotRepositoryPort = productSnapshotRepositoryPort;
        this.productStockRepositoryPort = productStockRepositoryPort;
        this.checkoutRequestRepositoryPort = checkoutRequestRepositoryPort;
    }

    // idempotencyKey is optional (null or blank = none). With a key, sending the same request again
    // returns the order the first one created instead of failing with CART_EMPTY.
    public OrderResult checkout(String buyerId, String idempotencyKey) {
        String key = normalizedKey(idempotencyKey);

        // Lock the cart FIRST: a second request for the same buyer waits here until the first one
        // has committed, and then sees its result (the order for this key, or an emptied cart).
        Optional<Cart> lockedCart = cartRepositoryPort.findByBuyerIdForUpdate(buyerId);

        if (key != null) {
            Optional<Order> alreadyCreated = checkoutRequestRepositoryPort.findOrderId(buyerId, key)
                    .flatMap(orderRepositoryPort::findById);
            if (alreadyCreated.isPresent()) {
                return OrderResult.from(alreadyCreated.get());
            }
        }

        Cart cart = lockedCart.orElseGet(() -> Cart.create(buyerId));
        if (cart.isEmpty()) {
            throw new ConflictException("CART_EMPTY", "Cannot checkout an empty cart");
        }

        List<OrderItem> items = cart.getItems().stream().map(this::repriced).map(OrderItem::fromCartItem).toList();
        Order order = Order.create(buyerId, null, items);
        Order saved = orderRepositoryPort.save(order);
        cartRepositoryPort.save(cart.clear());
        if (key != null) {
            checkoutRequestRepositoryPort.save(buyerId, key, saved.getId());
        }

        eventPublisherPort.publish(new OrderCreatedEvent(saved.getId(), null, null,
                saved.singleSellerIdOrNull(), saved.getBuyerId(), saved.getTotalAmount()));
        eventPublisherPort.publish(new InventoryReservationRequestedEvent(saved.getId(), toLineItems(items)));
        return OrderResult.from(saved);
    }

    private static String normalizedKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String key = idempotencyKey.trim();
        if (key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new ValidationException(List.of(new FieldError("Idempotency-Key",
                    "must be at most " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters")));
        }
        return key;
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
