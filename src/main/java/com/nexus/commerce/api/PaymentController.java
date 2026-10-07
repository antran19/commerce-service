package com.nexus.commerce.api;

import com.nexus.commerce.api.dto.request.ConfirmPaymentRequest;
import com.nexus.commerce.api.dto.request.CreateCheckoutSessionRequest;
import com.nexus.commerce.api.dto.response.CheckoutSessionResponse;
import com.nexus.commerce.api.dto.response.OrderResponse;
import com.nexus.commerce.api.mapper.CommerceApiMapper;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.application.usecase.ConfirmPaymentUseCase;
import com.nexus.commerce.application.usecase.CreateCheckoutSessionUseCase;
import com.nexus.commerce.application.usecase.OrderResult;
import com.nexus.common.core.ApiResponse;
import com.nexus.common.security.RequiresPrivilege;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders/{orderId}")
public class PaymentController {

    private final CreateCheckoutSessionUseCase createCheckoutSessionUseCase;
    private final ConfirmPaymentUseCase confirmPaymentUseCase;
    private final CommerceApiMapper mapper;

    public PaymentController(CreateCheckoutSessionUseCase createCheckoutSessionUseCase,
                              ConfirmPaymentUseCase confirmPaymentUseCase, CommerceApiMapper mapper) {
        this.createCheckoutSessionUseCase = createCheckoutSessionUseCase;
        this.confirmPaymentUseCase = confirmPaymentUseCase;
        this.mapper = mapper;
    }

    // Reuses CHECKOUT.START -- paying for an order you already own is the natural continuation
    // of the checkout flow every BUYER already has by default; it does not warrant its own
    // privilege, same reasoning auction-service used for reusing AUCTION.BID.
    @RequiresPrivilege("CHECKOUT.START")
    @PostMapping("/checkout-session")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> createCheckoutSession(
            Authentication authentication, @PathVariable String orderId,
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        String buyerId = buyerId(authentication);
        PaymentGatewayPort.CheckoutSession session = createCheckoutSessionUseCase.createSession(
                orderId, buyerId, request.successUrl(), request.cancelUrl());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(session)));
    }

    @RequiresPrivilege("CHECKOUT.START")
    @PostMapping("/confirm-payment")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPayment(
            Authentication authentication, @PathVariable String orderId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        String buyerId = buyerId(authentication);
        OrderResult result = confirmPaymentUseCase.confirm(orderId, buyerId, request.sessionId());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    private static String buyerId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
