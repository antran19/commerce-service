package com.nexus.commerce.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Cart {

    private final String id;
    private final String buyerId;
    private final List<CartItem> items;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Cart(String id, String buyerId, List<CartItem> items, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.buyerId = buyerId;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Cart create(String buyerId) {
        Instant now = Instant.now();
        return new Cart(UUID.randomUUID().toString(), buyerId, new ArrayList<>(), now, now);
    }

    public static Cart reconstitute(String id, String buyerId, List<CartItem> items,
                                     Instant createdAt, Instant updatedAt) {
        return new Cart(id, buyerId, new ArrayList<>(items), createdAt, updatedAt);
    }

    // Re-adding a product already in the cart merges quantities and refreshes the price/name
    // snapshot to the caller's values, since the FE always sends the product's current price.
    public Cart addItem(CartItem newItem) {
        List<CartItem> updated = new ArrayList<>();
        boolean merged = false;
        for (CartItem existing : items) {
            if (existing.getProductId().equals(newItem.getProductId())) {
                updated.add(newItem.withQuantity(existing.getQuantity() + newItem.getQuantity()));
                merged = true;
            } else {
                updated.add(existing);
            }
        }
        if (!merged) {
            updated.add(newItem);
        }
        return new Cart(id, buyerId, updated, createdAt, Instant.now());
    }

    public Cart updateItemQuantity(String productId, int newQuantity) {
        List<CartItem> updated = new ArrayList<>();
        for (CartItem existing : items) {
            if (existing.getProductId().equals(productId)) {
                updated.add(existing.withQuantity(newQuantity));
            } else {
                updated.add(existing);
            }
        }
        return new Cart(id, buyerId, updated, createdAt, Instant.now());
    }

    public Cart removeItem(String productId) {
        List<CartItem> updated = new ArrayList<>();
        for (CartItem existing : items) {
            if (!existing.getProductId().equals(productId)) {
                updated.add(existing);
            }
        }
        return new Cart(id, buyerId, updated, createdAt, Instant.now());
    }

    public Cart clear() {
        return new Cart(id, buyerId, new ArrayList<>(), createdAt, Instant.now());
    }

    public Optional<CartItem> findItem(String productId) {
        return items.stream().filter(i -> i.getProductId().equals(productId)).findFirst();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public BigDecimal totalAmount() {
        return items.stream().map(CartItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getId() { return id; }
    public String getBuyerId() { return buyerId; }
    public List<CartItem> getItems() { return List.copyOf(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
