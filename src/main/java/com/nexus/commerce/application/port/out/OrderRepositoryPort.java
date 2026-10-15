package com.nexus.commerce.application.port.out;

import com.nexus.commerce.domain.model.Order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    Optional<Order> findById(String id);
    Optional<Order> findByIdForUpdate(String id);
    Optional<Order> findByAuctionId(String auctionId);
    List<Order> findByBuyerId(String buyerId);
    List<Order> findBySellerId(String sellerId);
    List<Order> findAll();
    // AWAITING_PAYMENT orders from direct checkout (no auctionId) created before the cutoff.
    List<Order> findUnpaidDirectOrdersCreatedBefore(Instant cutoff);
    Order save(Order order);
}
