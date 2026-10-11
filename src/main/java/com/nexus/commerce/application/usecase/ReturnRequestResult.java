package com.nexus.commerce.application.usecase;

import com.nexus.commerce.domain.model.ReturnRequest;

import java.time.Instant;

public record ReturnRequestResult(String id, String orderId, String reason, String status, Instant createdAt,
                                   Instant resolvedAt, String resolvedBy) {

    public static ReturnRequestResult from(ReturnRequest returnRequest) {
        return new ReturnRequestResult(returnRequest.getId(), returnRequest.getOrderId(), returnRequest.getReason(),
                returnRequest.getStatus().name(), returnRequest.getCreatedAt(), returnRequest.getResolvedAt(),
                returnRequest.getResolvedBy());
    }
}
