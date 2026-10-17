package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.infrastructure.persistence.entity.ProductSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface ProductSnapshotJpaRepository extends JpaRepository<ProductSnapshotJpaEntity, String> {

    @Modifying
    @Query(value = "INSERT INTO product_snapshots (product_id, seller_id, name, price, status, updated_at) "
            + "VALUES (:productId, :sellerId, :name, :price, :status, :updatedAt) "
            + "ON CONFLICT (product_id) DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("productId") String productId, @Param("sellerId") String sellerId,
                        @Param("name") String name, @Param("price") BigDecimal price,
                        @Param("status") String status, @Param("updatedAt") Instant updatedAt);
}
