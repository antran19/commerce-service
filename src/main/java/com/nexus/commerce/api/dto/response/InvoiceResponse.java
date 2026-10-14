package com.nexus.commerce.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InvoiceResponse(String invoiceNumber, String orderId, String buyerId, List<OrderItemResponse> items,
                               BigDecimal totalAmount, String orderStatus, Instant issuedAt) {
}
