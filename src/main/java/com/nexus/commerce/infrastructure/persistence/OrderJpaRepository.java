package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.infrastructure.persistence.entity.OrderJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.id = :id")
    Optional<OrderJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<OrderJpaEntity> findByAuctionId(String auctionId);

    List<OrderJpaEntity> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    @Query("SELECT DISTINCT o FROM OrderJpaEntity o JOIN o.items i WHERE i.sellerId = :sellerId "
            + "ORDER BY o.createdAt DESC")
    List<OrderJpaEntity> findBySellerId(@Param("sellerId") String sellerId);

    List<OrderJpaEntity> findAllByOrderByCreatedAtDesc();
}
