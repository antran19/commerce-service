package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.domain.model.ProductSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

// Keeps commerce-service's copy of catalog products in step with catalog-events, so cart prices
// and seller ids come from here and not from the client. Catalog publishes every event of a
// product with the product id as Kafka key, so they arrive in order for that product.
public class SyncProductSnapshotUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncProductSnapshotUseCase.class);

    private final ProductSnapshotRepositoryPort repositoryPort;

    public SyncProductSnapshotUseCase(ProductSnapshotRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public void onCreated(String productId, String sellerId, String name, BigDecimal price) {
        // Events published before ProductCreated carried a price (common-libs < 1.12.0) cannot
        // be turned into a snapshot; such a product stays unavailable for carts.
        if (price == null || sellerId == null) {
            log.warn("ProductCreated for {} has no price or seller; product stays unavailable for carts", productId);
            return;
        }
        repositoryPort.insertIfAbsent(ProductSnapshot.create(productId, sellerId, name, price));
    }

    public void onStatusChanged(String productId, String newStatus) {
        repositoryPort.findById(productId).ifPresentOrElse(
                s -> repositoryPort.save(s.withStatus(newStatus)),
                () -> log.warn("Status change for {} ignored: no snapshot of this product", productId));
    }

    public void onRenamed(String productId, String newName) {
        repositoryPort.findById(productId).ifPresentOrElse(
                s -> repositoryPort.save(s.withName(newName)),
                () -> log.warn("Rename of {} ignored: no snapshot of this product", productId));
    }
}
