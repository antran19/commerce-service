package com.nexus.commerce.application.usecase;

import com.nexus.common.core.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedirectUrlPolicyTest {

    private final RedirectUrlPolicy policy =
            new RedirectUrlPolicy(List.of("http://localhost:5173", " https://shop.example.com "));

    private void rejected(String url) {
        assertThatThrownBy(() -> policy.requireAllowed("successUrl", url))
                .as("should be rejected: %s", url)
                .isInstanceOf(ValidationException.class);
    }

    private void accepted(String url) {
        assertThatCode(() -> policy.requireAllowed("successUrl", url))
                .as("should be accepted: %s", url).doesNotThrowAnyException();
    }

    @Test
    void addressesOnTheFrontEndOrigin_areAccepted() {
        accepted("http://localhost:5173/payment/success");
        accepted("http://localhost:5173");
        accepted("http://localhost:5173/pay?orderId=1&x=y#top");
        accepted("https://shop.example.com/done");
    }

    @Test
    void theComparisonIgnoresCaseAndDefaultPorts() {
        accepted("HTTPS://SHOP.EXAMPLE.COM/done");
        accepted("https://shop.example.com:443/done");
    }

    @Test
    void stripesSessionPlaceholder_doesNotBreakParsing() {
        accepted("http://localhost:5173/done?session_id={CHECKOUT_SESSION_ID}");
    }

    @Test
    void anotherSite_isRejected() {
        rejected("https://evil.example.org/phish");
        rejected("http://shop.example.com/done");          // same host, other scheme
        rejected("https://shop.example.com:8443/done");    // same host, other port
        rejected("http://localhost:3000/ok");              // not in this policy's list
    }

    // Starting with an allowed origin as TEXT is not the same as being on it.
    @Test
    void lookAlikeAddresses_areRejected() {
        rejected("http://localhost:5173.evil.com/x");
        rejected("http://localhost:5173@evil.com/x");
        rejected("http://localhost:5173:80@evil.com/x");
        rejected("https://shop.example.com.evil.com/x");
        rejected("https://evil.com/https://shop.example.com");
        rejected("https://evil.com\\@shop.example.com/");
    }

    @Test
    void schemesThatAreNotHttp_areRejected() {
        rejected("javascript:alert(1)");
        rejected("data:text/html,<script>alert(1)</script>");
        rejected("ftp://shop.example.com/x");
        rejected("file:///etc/passwd");
    }

    @Test
    void relativeProtocolRelativeAndGarbage_areRejected() {
        rejected("/payment/success");
        rejected("//evil.com/x");
        rejected("not a url");
        rejected("");
        rejected("   ");
        rejected(null);
    }

    @Test
    void theErrorNamesTheFieldAndTheAllowedOrigins() {
        assertThatThrownBy(() -> policy.requireAllowed("cancelUrl", "https://evil.example.org"))
                .isInstanceOfSatisfying(ValidationException.class, e -> {
                    org.assertj.core.api.Assertions.assertThat(e.getFieldErrors()).hasSize(1);
                    org.assertj.core.api.Assertions.assertThat(e.getFieldErrors().get(0).field()).isEqualTo("cancelUrl");
                    org.assertj.core.api.Assertions.assertThat(e.getFieldErrors().get(0).message())
                            .contains("http://localhost:5173");
                });
    }

    // Nothing configured must mean nothing allowed, never "everything allowed".
    @Test
    void withNoOriginConfigured_everythingIsRejected() {
        RedirectUrlPolicy closed = new RedirectUrlPolicy(List.of("", "  "));

        assertThatThrownBy(() -> closed.requireAllowed("successUrl", "http://localhost:5173/ok"))
                .isInstanceOf(ValidationException.class);
    }
}
