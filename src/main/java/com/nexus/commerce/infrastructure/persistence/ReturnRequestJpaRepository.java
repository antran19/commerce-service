package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.infrastructure.persistence.entity.ReturnRequestJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReturnRequestJpaRepository extends JpaRepository<ReturnRequestJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReturnRequestJpaEntity r WHERE r.id = :id")
    Optional<ReturnRequestJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    List<ReturnRequestJpaEntity> findByOrderId(UUID orderId);
}
