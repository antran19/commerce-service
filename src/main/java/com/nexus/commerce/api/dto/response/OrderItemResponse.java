package com.nexus.commerce.api.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(String productId, String sellerId, String productName,
                                 BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
}
