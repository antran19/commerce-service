package com.nexus.commerce.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkout_requests")
public class CheckoutRequestJpaEntity {

    @Id
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CheckoutRequestJpaEntity() {
    }

    public CheckoutRequestJpaEntity(UUID id, String buyerId, String idempotencyKey, UUID orderId, Instant createdAt) {
        this.id = id;
        this.buyerId = buyerId;
        this.idempotencyKey = idempotencyKey;
        this.orderId = orderId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getBuyerId() { return buyerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getOrderId() { return orderId; }
    public Instant getCreatedAt() { return createdAt; }
}
