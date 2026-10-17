package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.infrastructure.persistence.entity.ProductStockLevelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProductStockLevelJpaRepository extends JpaRepository<ProductStockLevelJpaEntity, String> {

    // One statement, so two deliveries racing each other cannot both win: the row only changes
    // when the incoming level was reported at the same time or later than the stored one.
    @Modifying
    @Query(value = "INSERT INTO product_stock_levels (product_id, available_quantity, as_of) "
            + "VALUES (:productId, :available, :asOf) "
            + "ON CONFLICT (product_id) DO UPDATE SET available_quantity = EXCLUDED.available_quantity, "
            + "as_of = EXCLUDED.as_of WHERE product_stock_levels.as_of <= EXCLUDED.as_of", nativeQuery = true)
    void applyIfNewer(@Param("productId") String productId, @Param("available") int available,
                      @Param("asOf") Instant asOf);
}
