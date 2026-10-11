package com.nexus.commerce.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Order {

    private final String id;
    private final String buyerId;
    private final String auctionId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final OrderStatus status;
    private final Instant createdAt;
    private final Instant paidAt;
    // The Stripe PaymentIntent id behind this order's payment -- null until markPaid, needed
    // later to refund the exact charge (see RefundOrderUseCase).
    private final String paymentReference;

    private Order(String id, String buyerId, String auctionId, List<OrderItem> items, BigDecimal totalAmount,
                   OrderStatus status, Instant createdAt, Instant paidAt, String paymentReference) {
        this.id = id;
        this.buyerId = buyerId;
        this.auctionId = auctionId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
        this.paymentReference = paymentReference;
    }

    // auctionId is null for a direct-purchase (cart/checkout) order; set when the order was
    // created from a settled auction's AuctionWonEvent.
    public static Order create(String buyerId, String auctionId, List<OrderItem> items) {
        BigDecimal total = items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Order(UUID.randomUUID().toString(), buyerId, auctionId, List.copyOf(items), total,
                OrderStatus.AWAITING_PAYMENT, Instant.now(), null, null);
    }

    public static Order reconstitute(String id, String buyerId, String auctionId, List<OrderItem> items,
                                      BigDecimal totalAmount, OrderStatus status, Instant createdAt, Instant paidAt,
                                      String paymentReference) {
        return new Order(id, buyerId, auctionId, List.copyOf(items), totalAmount, status, createdAt, paidAt,
                paymentReference);
    }

    public Order markPaid(Instant when, String paymentReference) {
        return new Order(id, buyerId, auctionId, items, totalAmount, OrderStatus.PAID, createdAt, when,
                paymentReference);
    }

    public Order cancel() {
        return new Order(id, buyerId, auctionId, items, totalAmount, OrderStatus.CANCELLED, createdAt, paidAt,
                paymentReference);
    }

    // Money only -- whether the physical goods come back to stock is a separate, manual
    // decision (see HANDOFF.md): this service has no shipment tracking yet to know whether
    // the item ever left the warehouse, so it can't safely automate that part.
    public Order refund() {
        return new Order(id, buyerId, auctionId, items, totalAmount, OrderStatus.REFUNDED, createdAt, paidAt,
                paymentReference);
    }

    // Null when the order spans items from more than one seller (a direct-purchase cart can
    // mix sellers); callers that need a single seller id -- e.g. the OrderPaid event -- must
    // handle that case explicitly rather than guessing one seller.
    public String singleSellerIdOrNull() {
        String first = items.isEmpty() ? null : items.get(0).getSellerId();
        boolean allSame = items.stream().allMatch(i -> i.getSellerId().equals(first));
        return allSame ? first : null;
    }

    public String getId() { return id; }
    public String getBuyerId() { return buyerId; }
    public String getAuctionId() { return auctionId; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public String getPaymentReference() { return paymentReference; }
}
