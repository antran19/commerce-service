package com.nexus.commerce.api;

import com.nexus.commerce.api.mapper.CommerceApiMapperImpl;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.application.usecase.ConfirmPaymentUseCase;
import com.nexus.commerce.application.usecase.CreateCheckoutSessionUseCase;
import com.nexus.commerce.application.usecase.OrderResult;
import com.nexus.commerce.infrastructure.config.SecurityConfig;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.security.JwtAuthenticationFilter;
import com.nexus.common.security.JwtTokenProvider;
import com.nexus.common.security.PrivilegeAuthorizationAspect;
import com.nexus.common.web.GlobalExceptionHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, CommerceApiMapperImpl.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, PrivilegeAuthorizationAspect.class})
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CreateCheckoutSessionUseCase createCheckoutSessionUseCase;
    @MockBean private ConfirmPaymentUseCase confirmPaymentUseCase;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    private void authenticateAs(String subject, String... privileges) {
        when(jwtTokenProvider.isValid("good-token")).thenReturn(true);
        Claims claims = Jwts.claims().subject(subject).add("privileges", List.of(privileges)).build();
        when(jwtTokenProvider.parseClaims("good-token")).thenReturn(claims);
    }

    @Test
    void createCheckoutSession_returns200() throws Exception {
        authenticateAs("buyer-1", "CHECKOUT.START");
        when(createCheckoutSessionUseCase.createSession("order-1", "buyer-1",
                "https://fe/success", "https://fe/cancel"))
                .thenReturn(new PaymentGatewayPort.CheckoutSession("sess-1", "https://checkout.stripe.com/sess-1"));

        mockMvc.perform(post("/api/v1/orders/order-1/checkout-session")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"successUrl":"https://fe/success","cancelUrl":"https://fe/cancel"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://checkout.stripe.com/sess-1"));
    }

    @Test
    void confirmPayment_returns200() throws Exception {
        authenticateAs("buyer-1", "CHECKOUT.START");
        when(confirmPaymentUseCase.confirm("order-1", "buyer-1", "sess-1"))
                .thenReturn(new OrderResult("order-1", "buyer-1", null, List.of(), new BigDecimal("150.00"),
                        "PAID", Instant.now(), Instant.now(), null));

        mockMvc.perform(post("/api/v1/orders/order-1/confirm-payment")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"sessionId":"sess-1"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    void confirmPayment_returns409WhenStripeHasNotConfirmed() throws Exception {
        authenticateAs("buyer-1", "CHECKOUT.START");
        when(confirmPaymentUseCase.confirm("order-1", "buyer-1", "sess-1"))
                .thenThrow(new ConflictException("PAYMENT_NOT_CONFIRMED", "Stripe has not confirmed this payment yet"));

        mockMvc.perform(post("/api/v1/orders/order-1/confirm-payment")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"sessionId":"sess-1"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_NOT_CONFIRMED"));
    }
}
