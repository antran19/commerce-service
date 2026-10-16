package com.nexus.commerce.application.exception;

import com.nexus.common.core.exception.UnauthorizedException;

// The webhook endpoint has no JWT -- the gateway's signature is its only proof of origin, so a
// missing/invalid one is treated like a failed authentication (401).
public class InvalidWebhookSignatureException extends UnauthorizedException {
    public InvalidWebhookSignatureException(String message) {
        super("INVALID_WEBHOOK_SIGNATURE", message);
    }
}
