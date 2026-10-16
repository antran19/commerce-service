package com.nexus.commerce.api;

import com.nexus.commerce.application.exception.InvalidWebhookSignatureException;
import com.nexus.commerce.application.usecase.HandlePaymentWebhookUseCase;
import com.nexus.commerce.infrastructure.config.SecurityConfig;
import com.nexus.common.security.JwtAuthenticationFilter;
import com.nexus.common.security.JwtTokenProvider;
import com.nexus.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentWebhookController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class PaymentWebhookControllerTest {

    private static final String BODY = "{\"type\":\"checkout.session.completed\"}";

    @Autowired private MockMvc mockMvc;

    @MockBean private HandlePaymentWebhookUseCase handlePaymentWebhookUseCase;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    void webhook_needsNoJwtAndPassesRawBodyThrough() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("Stripe-Signature", "t=1,v1=abc")
                        .contentType(APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());

        verify(handlePaymentWebhookUseCase).handle(BODY, "t=1,v1=abc");
    }

    @Test
    void webhook_returns401WhenSignatureInvalid() throws Exception {
        doThrow(new InvalidWebhookSignatureException("bad"))
                .when(handlePaymentWebhookUseCase).handle(BODY, "t=1,v1=bad");

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("Stripe-Signature", "t=1,v1=bad")
                        .contentType(APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_WEBHOOK_SIGNATURE"));
    }
}
