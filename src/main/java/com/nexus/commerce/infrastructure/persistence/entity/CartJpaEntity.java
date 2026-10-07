package com.nexus.commerce.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class CartJpaEntity {

    @Id
    private UUID id;

    @Column(name = "buyer_id", nullable = false, unique = true)
    private String buyerId;

    @ElementCollection
    @CollectionTable(name = "cart_items", joinColumns = @JoinColumn(name = "cart_id"))
    private List<CartItemEmbeddable> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartJpaEntity() {
    }

    public CartJpaEntity(UUID id, String buyerId, List<CartItemEmbeddable> items,
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.buyerId = buyerId;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getBuyerId() { return buyerId; }
    public List<CartItemEmbeddable> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
