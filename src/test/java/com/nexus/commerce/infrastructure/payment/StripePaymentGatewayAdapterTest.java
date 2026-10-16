package com.nexus.commerce.infrastructure.payment;

import com.nexus.commerce.application.exception.InvalidWebhookSignatureException;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripePaymentGatewayAdapterTest {

    private static final String SECRET = "whsec_test_secret";

    private static String event(String type) {
        return """
                {"id":"evt_1","object":"event","type":"%s","data":{"object":{"id":"cs_test_1","object":"checkout.session"}}}"""
                .formatted(type);
    }

    // Same header format Stripe sends: t=<unix seconds>,v1=<HMAC-SHA256 of "t.payload">.
    private static String sign(String payload, String secret) throws Exception {
        long t = Instant.now().getEpochSecond();
        return "t=" + t + ",v1=" + Webhook.Util.computeHmacSha256(secret, t + "." + payload);
    }

    @Test
    void expiresAt_isWithinStripesAllowedWindowOfThirtyMinutesToTwentyFourHours() {
        Instant now = Instant.parse("2026-10-15T03:00:00Z");

        long seconds = StripePaymentGatewayAdapter.expiresAt(now) - now.getEpochSecond();

        assertThat(seconds).isGreaterThanOrEqualTo(Duration.ofMinutes(30).toSeconds());
        assertThat(seconds).isLessThanOrEqualTo(Duration.ofHours(24).toSeconds());
    }

    @Test
    void webhook_returnsSessionIdForSignedCompletedEvent() throws Exception {
        String payload = event("checkout.session.completed");

        assertThat(new StripePaymentGatewayAdapter(SECRET).verifyWebhookAndExtractSessionId(payload, sign(payload, SECRET)))
                .contains("cs_test_1");
    }

    @Test
    void webhook_ignoresOtherEventTypes() throws Exception {
        String payload = event("checkout.session.expired");

        assertThat(new StripePaymentGatewayAdapter(SECRET).verifyWebhookAndExtractSessionId(payload, sign(payload, SECRET)))
                .isEmpty();
    }

    @Test
    void webhook_rejectsWrongSignature() throws Exception {
        String payload = event("checkout.session.completed");
        String signedWithOtherSecret = sign(payload, "whsec_someone_else");

        assertThatThrownBy(() -> new StripePaymentGatewayAdapter(SECRET)
                .verifyWebhookAndExtractSessionId(payload, signedWithOtherSecret))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    void webhook_rejectsTamperedBody() throws Exception {
        String signature = sign(event("checkout.session.completed"), SECRET);
        String tampered = event("checkout.session.completed").replace("cs_test_1", "cs_test_2");

        assertThatThrownBy(() -> new StripePaymentGatewayAdapter(SECRET)
                .verifyWebhookAndExtractSessionId(tampered, signature))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    void webhook_rejectsEverythingWhenSecretNotConfigured() throws Exception {
        String payload = event("checkout.session.completed");
        String signature = sign(payload, SECRET);
        StripePaymentGatewayAdapter unconfigured = new StripePaymentGatewayAdapter("");

        assertThatThrownBy(() -> unconfigured.verifyWebhookAndExtractSessionId(payload, signature))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    void webhook_rejectsMissingSignatureHeader() {
        assertThatThrownBy(() -> new StripePaymentGatewayAdapter(SECRET)
                .verifyWebhookAndExtractSessionId(event("checkout.session.completed"), null))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }
}
