package com.nexus.commerce.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InvoiceResult(String invoiceNumber, String orderId, String buyerId, List<OrderItemResult> items,
                             BigDecimal totalAmount, String orderStatus, Instant issuedAt) {
}
