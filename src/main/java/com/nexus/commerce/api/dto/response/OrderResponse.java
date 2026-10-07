package com.nexus.commerce.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(String id, String buyerId, String auctionId, List<OrderItemResponse> items,
                             BigDecimal totalAmount, String status, Instant createdAt, Instant paidAt) {
}
