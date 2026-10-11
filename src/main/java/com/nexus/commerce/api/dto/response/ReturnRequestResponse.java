package com.nexus.commerce.api.dto.response;

import java.time.Instant;

public record ReturnRequestResponse(String id, String orderId, String reason, String status, Instant createdAt,
                                     Instant resolvedAt, String resolvedBy) {
}
