package com.nexus.commerce.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @Column(name = "auction_id")
    private String auctionId;

    // EAGER: open-in-view is disabled, so the session is gone by the time the adapter maps
    // this entity back to the domain Order -- a lazy collection would throw outside the
    // transaction, and these lists are always small and always needed anyway.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItemEmbeddable> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    protected OrderJpaEntity() {
    }

    public OrderJpaEntity(UUID id, String buyerId, String auctionId, List<OrderItemEmbeddable> items,
                           BigDecimal totalAmount, String status, Instant createdAt, Instant paidAt,
                           String paymentReference, Instant shippedAt) {
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
    }

    public UUID getId() { return id; }
    public String getBuyerId() { return buyerId; }
    public String getAuctionId() { return auctionId; }
    public List<OrderItemEmbeddable> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public String getPaymentReference() { return paymentReference; }
    public Instant getShippedAt() { return shippedAt; }
}
