package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.infrastructure.persistence.entity.CartJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {
    Optional<CartJpaEntity> findByBuyerId(String buyerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartJpaEntity c WHERE c.buyerId = :buyerId")
    Optional<CartJpaEntity> findByBuyerIdForUpdate(@Param("buyerId") String buyerId);

    // Bulk delete: cart_items rows go with it via ON DELETE CASCADE (V1 migration).
    @Transactional
    @Modifying
    @Query("DELETE FROM CartJpaEntity c WHERE c.updatedAt < :cutoff")
    int deleteByUpdatedAtBefore(@Param("cutoff") Instant cutoff);
}
