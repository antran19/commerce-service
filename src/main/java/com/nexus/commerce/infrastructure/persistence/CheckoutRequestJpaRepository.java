package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.infrastructure.persistence.entity.CheckoutRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutRequestJpaRepository extends JpaRepository<CheckoutRequestJpaEntity, UUID> {

    Optional<CheckoutRequestJpaEntity> findByBuyerIdAndIdempotencyKey(String buyerId, String idempotencyKey);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CheckoutRequestJpaEntity c WHERE c.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
