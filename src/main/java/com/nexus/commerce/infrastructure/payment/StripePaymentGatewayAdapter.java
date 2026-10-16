package com.nexus.commerce.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.commerce.application.exception.InvalidWebhookSignatureException;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    // VND is one of Stripe's zero-decimal currencies -- unlike USD, unit_amount is the whole
    // VND amount itself, NOT the amount * 100. Multiplying here would overcharge 100x.
    private static final String CURRENCY = "vnd";

    // Stripe's default session lifetime is 24h, far longer than ExpireUnpaidOrdersUseCase's
    // window: a buyer could pay on a Stripe page for an order we already cancelled. Stripe
    // requires expires_at to be at least 30 minutes ahead (and at most 24h), so use the
    // minimum plus a 30s margin for clock skew/latency -- the closest we can get to the order's
    // own unpaid timeout.
    static final Duration SESSION_LIFETIME = Duration.ofMinutes(30).plusSeconds(30);

    // Both mean "the session's payment may now be complete"; retrieveSessionStatus is still what
    // decides whether it really is paid. Every other event type is acknowledged and ignored.
    static final Set<String> PAYMENT_EVENT_TYPES = Set.of(
            "checkout.session.completed", "checkout.session.async_payment_succeeded");

    private static final ObjectMapper JSON = new ObjectMapper();

    // whsec_... from the Stripe dashboard (or `stripe listen` locally). Blank = webhook disabled:
    // every call is rejected rather than accepted unverified.
    private final String webhookSecret;

    public StripePaymentGatewayAdapter(@Value("${nexus.payment.stripe.webhook-secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    static long expiresAt(Instant now) {
        return now.plus(SESSION_LIFETIME).getEpochSecond();
    }

    @Override
    public CheckoutSession createCheckoutSession(String orderId, String buyerId, List<LineItem> lineItems,
                                                  String successUrl, String cancelUrl) {
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + (successUrl.contains("?") ? "&" : "?") + "session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setExpiresAt(expiresAt(Instant.now()))
                .putMetadata("orderId", orderId)
                .putMetadata("buyerId", buyerId);

        for (LineItem item : lineItems) {
            long unitAmount = item.unitPrice().setScale(0, RoundingMode.HALF_UP).longValueExact();
            paramsBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity((long) item.quantity())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(CURRENCY)
                            .setUnitAmount(unitAmount)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(item.name())
                                    .build())
                            .build())
                    .build());
        }

        try {
            Session session = Session.create(paramsBuilder.build());
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException e) {
            // Stripe's own exception is a checked exception with no domain meaning here; wrap
            // it so a Stripe outage/misconfiguration surfaces as a normal 500, not an unmapped
            // checked-exception compile error one layer up.
            throw new PaymentGatewayException("Failed to create Stripe checkout session for order " + orderId, e);
        }
    }

    @Override
    public PaymentStatus retrieveSessionStatus(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            boolean paid = "paid".equals(session.getPaymentStatus());
            String orderId = session.getMetadata() == null ? null : session.getMetadata().get("orderId");
            String buyerId = session.getMetadata() == null ? null : session.getMetadata().get("buyerId");
            return new PaymentStatus(paid, orderId, buyerId, session.getPaymentIntent());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to retrieve Stripe checkout session " + sessionId, e);
        }
    }

    @Override
    public void refund(String paymentReference) {
        try {
            // Idempotency key per charge: if our DB write fails after Stripe already refunded and
            // the call is retried (webhook redelivery, admin clicking again), Stripe returns the
            // first refund instead of erroring with "already refunded".
            Refund.create(RefundCreateParams.builder().setPaymentIntent(paymentReference).build(),
                    RequestOptions.builder().setIdempotencyKey("refund-" + paymentReference).build());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to refund Stripe payment " + paymentReference, e);
        }
    }

    @Override
    public Optional<String> verifyWebhookAndExtractSessionId(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new InvalidWebhookSignatureException("Stripe webhook secret is not configured");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new InvalidWebhookSignatureException("Missing Stripe-Signature header");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new InvalidWebhookSignatureException("Stripe webhook signature does not match");
        }
        if (!PAYMENT_EVENT_TYPES.contains(event.getType())) {
            return Optional.empty();
        }
        // Read the id straight from the (now verified) JSON instead of Stripe's typed
        // deserializer, which returns empty when the event's API version differs from the SDK's.
        try {
            JsonNode id = JSON.readTree(payload).path("data").path("object").path("id");
            return id.isTextual() ? Optional.of(id.asText()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
