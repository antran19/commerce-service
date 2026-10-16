package com.nexus.commerce.api;

import com.nexus.commerce.application.usecase.HandlePaymentWebhookUseCase;
import com.nexus.common.core.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Called by Stripe, not by a logged-in user: no JWT and no @RequiresPrivilege (SecurityConfig
// permits this path). The Stripe-Signature header, checked against the raw body, is the only
// authentication -- which is why the body is taken as a plain String: re-serializing a parsed
// object would change the bytes and break the signature.
@RestController
@RequestMapping("/api/v1/payments/webhook")
public class PaymentWebhookController {

    private final HandlePaymentWebhookUseCase handlePaymentWebhookUseCase;

    public PaymentWebhookController(HandlePaymentWebhookUseCase handlePaymentWebhookUseCase) {
        this.handlePaymentWebhookUseCase = handlePaymentWebhookUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> receive(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        handlePaymentWebhookUseCase.handle(payload, signature);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
