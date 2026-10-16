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
    // Set by the Seller via markShipped -- there's no real carrier/tracking integration, just
    // a manual "I shipped this" flag. Lets RefundOrderUseCase decide whether it's still safe
    // to auto-release the reservation (not shipped yet) or whether goods handling needs the
    // manual Return flow instead (see ReturnRequest).
    private final Instant shippedAt;
    // Null until the buyer sets it (PUT /orders/{id}/shipping-address); required before a
    // payment session can be created. Orders created before V7 keep it null.
    private final ShippingAddress shippingAddress;

    private Order(String id, String buyerId, String auctionId, List<OrderItem> items, BigDecimal totalAmount,
                   OrderStatus status, Instant createdAt, Instant paidAt, String paymentReference,
                   Instant shippedAt, ShippingAddress shippingAddress) {
        this.id = id;
        this.buyerId = buyerId;
        this.auctionId = auctionId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
        this.paymentReference = paymentReference;
        this.shippedAt = shippedAt;
        this.shippingAddress = shippingAddress;
    }

    // auctionId is null for a direct-purchase (cart/checkout) order; set when the order was
    // created from a settled auction's AuctionWonEvent.
    public static Order create(String buyerId, String auctionId, List<OrderItem> items) {
        BigDecimal total = items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Order(UUID.randomUUID().toString(), buyerId, auctionId, List.copyOf(items), total,
                OrderStatus.AWAITING_PAYMENT, Instant.now(), null, null, null, null);
    }

    public static Order reconstitute(String id, String buyerId, String auctionId, List<OrderItem> items,
                                      BigDecimal totalAmount, OrderStatus status, Instant createdAt, Instant paidAt,
                                      String paymentReference, Instant shippedAt) {
        return reconstitute(id, buyerId, auctionId, items, totalAmount, status, createdAt, paidAt,
                paymentReference, shippedAt, null);
    }

    public static Order reconstitute(String id, String buyerId, String auctionId, List<OrderItem> items,
                                      BigDecimal totalAmount, OrderStatus status, Instant createdAt, Instant paidAt,
                                      String paymentReference, Instant shippedAt, ShippingAddress shippingAddress) {
        return new Order(id, buyerId, auctionId, List.copyOf(items), totalAmount, status, createdAt, paidAt,
                paymentReference, shippedAt, shippingAddress);
    }

    public Order markPaid(Instant when, String paymentReference) {
        return new Order(id, buyerId, auctionId, items, totalAmount, OrderStatus.PAID, createdAt, when,
                paymentReference, shippedAt, shippingAddress);
    }

    public Order cancel() {
        return new Order(id, buyerId, auctionId, items, totalAmount, OrderStatus.CANCELLED, createdAt, paidAt,
                paymentReference, shippedAt, shippingAddress);
    }

    // Money only -- whether the physical goods come back to stock is decided separately: see
    // RefundOrderUseCase (auto-release if shippedAt is still null) and ReturnRequest (manual
    // restock once a shipped item is confirmed received back).
    public Order refund() {
        return new Order(id, buyerId, auctionId, items, totalAmount, OrderStatus.REFUNDED, createdAt, paidAt,
                paymentReference, shippedAt, shippingAddress);
    }

    // The buyer paid on a still-open Stripe page after this order was already cancelled (closed
    // tab + unpaid-timeout, or cancelled then paid anyway). The money is sent straight back, so
    // the order goes CANCELLED -> REFUNDED; paidAt/paymentReference record the charge that was
    // refunded (the invoice of a REFUNDED order needs paidAt). Stock is untouched: the
    // reservation was already released when the order was cancelled.
    public Order refundLatePayment(Instant when, String paymentReference) {
        if (status != OrderStatus.CANCELLED) {
            throw new IllegalStateException("Only a cancelled order can have a late payment refunded");
        }
        return new Order(id, buyerId, auctionId, items, totalAmount, OrderStatus.REFUNDED, createdAt, when,
                paymentReference, shippedAt, shippingAddress);
    }

    public Order markShipped(Instant when) {
        return new Order(id, buyerId, auctionId, items, totalAmount, status, createdAt, paidAt, paymentReference,
                when, shippingAddress);
    }

    public Order withShippingAddress(ShippingAddress address) {
        return new Order(id, buyerId, auctionId, items, totalAmount, status, createdAt, paidAt, paymentReference,
                shippedAt, address);
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
    public Instant getShippedAt() { return shippedAt; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
}
