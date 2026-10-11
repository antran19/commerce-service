package com.nexus.commerce.domain.model;

import java.time.Instant;
import java.util.UUID;

// Manual record of a shipped, refunded order's goods coming back -- see RefundOrderUseCase
// for why this is separate from the refund itself (no shipment tracking to confirm receipt
// automatically) and CommerceEventsListener (fulfillment-service) for how RECEIVED restocks.
public class ReturnRequest {

    private final String id;
    private final String orderId;
    private final String reason;
    private final ReturnRequestStatus status;
    private final Instant createdAt;
    private final Instant resolvedAt;
    private final String resolvedBy;

    private ReturnRequest(String id, String orderId, String reason, ReturnRequestStatus status, Instant createdAt,
                           Instant resolvedAt, String resolvedBy) {
        this.id = id;
        this.orderId = orderId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
    }

    public static ReturnRequest create(String orderId, String reason) {
        return new ReturnRequest(UUID.randomUUID().toString(), orderId, reason, ReturnRequestStatus.REQUESTED,
                Instant.now(), null, null);
    }

    public static ReturnRequest reconstitute(String id, String orderId, String reason, ReturnRequestStatus status,
                                              Instant createdAt, Instant resolvedAt, String resolvedBy) {
        return new ReturnRequest(id, orderId, reason, status, createdAt, resolvedAt, resolvedBy);
    }

    public ReturnRequest receive(String adminId) {
        return new ReturnRequest(id, orderId, reason, ReturnRequestStatus.RECEIVED, createdAt, Instant.now(),
                adminId);
    }

    public ReturnRequest reject(String adminId) {
        return new ReturnRequest(id, orderId, reason, ReturnRequestStatus.REJECTED, createdAt, Instant.now(),
                adminId);
    }

    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getReason() { return reason; }
    public ReturnRequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
}
