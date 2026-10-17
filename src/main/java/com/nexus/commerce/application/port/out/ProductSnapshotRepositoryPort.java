package com.nexus.commerce.application.port.out;

import com.nexus.commerce.domain.model.ProductSnapshot;

import java.util.Optional;

public interface ProductSnapshotRepositoryPort {
    Optional<ProductSnapshot> findById(String productId);

    // No-op when the product already has a snapshot, so a ProductCreated event delivered twice
    // cannot reset a status that a later ProductStatusChanged already moved on.
    void insertIfAbsent(ProductSnapshot snapshot);

    void save(ProductSnapshot snapshot);
}
