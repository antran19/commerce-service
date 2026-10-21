package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.application.port.out.CheckoutRequestRepositoryPort;
import com.nexus.commerce.infrastructure.persistence.entity.CheckoutRequestJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class CheckoutRequestRepositoryAdapter implements CheckoutRequestRepositoryPort {

    private final CheckoutRequestJpaRepository jpaRepository;

    public CheckoutRequestRepositoryAdapter(CheckoutRequestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<String> findOrderId(String buyerId, String idempotencyKey) {
        return jpaRepository.findByBuyerIdAndIdempotencyKey(buyerId, idempotencyKey)
                .map(r -> r.getOrderId().toString());
    }

    @Override
    public void save(String buyerId, String idempotencyKey, String orderId) {
        jpaRepository.save(new CheckoutRequestJpaEntity(UUID.randomUUID(), buyerId, idempotencyKey,
                UUID.fromString(orderId), Instant.now()));
    }

    @Override
    public int deleteOlderThan(Instant cutoff) {
        return jpaRepository.deleteByCreatedAtBefore(cutoff);
    }
}
