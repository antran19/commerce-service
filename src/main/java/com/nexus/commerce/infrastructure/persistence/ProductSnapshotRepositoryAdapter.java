package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.domain.model.ProductSnapshot;
import com.nexus.commerce.infrastructure.persistence.entity.ProductSnapshotJpaEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class ProductSnapshotRepositoryAdapter implements ProductSnapshotRepositoryPort {

    private final ProductSnapshotJpaRepository jpaRepository;

    public ProductSnapshotRepositoryAdapter(ProductSnapshotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ProductSnapshot> findById(String productId) {
        return jpaRepository.findById(productId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void insertIfAbsent(ProductSnapshot snapshot) {
        jpaRepository.insertIfAbsent(snapshot.getProductId(), snapshot.getSellerId(), snapshot.getName(),
                snapshot.getPrice(), snapshot.getStatus(), Instant.now());
    }

    @Override
    public void save(ProductSnapshot snapshot) {
        jpaRepository.save(new ProductSnapshotJpaEntity(snapshot.getProductId(), snapshot.getSellerId(),
                snapshot.getName(), snapshot.getPrice(), snapshot.getStatus(), Instant.now()));
    }

    private ProductSnapshot toDomain(ProductSnapshotJpaEntity entity) {
        return ProductSnapshot.reconstitute(entity.getProductId(), entity.getSellerId(), entity.getName(),
                entity.getPrice(), entity.getStatus());
    }
}
