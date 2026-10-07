package com.nexus.commerce.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(String id, String buyerId, List<CartItemResponse> items, BigDecimal totalAmount) {
}
