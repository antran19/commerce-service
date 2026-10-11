package com.nexus.commerce.application.usecase;

import com.nexus.commerce.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResult(String id, String buyerId, String auctionId, List<OrderItemResult> items,
                           BigDecimal totalAmount, String status, Instant createdAt, Instant paidAt,
                           Instant shippedAt) {

    public static OrderResult from(Order order) {
        List<OrderItemResult> items = order.getItems().stream().map(OrderItemResult::from).toList();
        return new OrderResult(order.getId(), order.getBuyerId(), order.getAuctionId(), items,
                order.getTotalAmount(), order.getStatus().name(), order.getCreatedAt(), order.getPaidAt(),
                order.getShippedAt());
    }
}
