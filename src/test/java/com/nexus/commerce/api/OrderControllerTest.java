package com.nexus.commerce.api;

import com.nexus.commerce.api.mapper.CommerceApiMapperImpl;
import com.nexus.commerce.application.usecase.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, CommerceApiMapperImpl.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, PrivilegeAuthorizationAspect.class})
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private StartCheckoutUseCase startCheckoutUseCase;
    @MockBean private GetOrderUseCase getOrderUseCase;
    @MockBean private ListOrdersUseCase listOrdersUseCase;
    @MockBean private ListAllOrdersUseCase listAllOrdersUseCase;
    @MockBean private CancelOrderUseCase cancelOrderUseCase;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    private void authenticateAs(String subject, String... privileges) {
        authenticateAsWithRole(subject, null, privileges);
    }

    private void authenticateAsWithRole(String subject, String role, String... privileges) {
        when(jwtTokenProvider.isValid("good-token")).thenReturn(true);
        var builder = Jwts.claims().subject(subject).add("privileges", List.of(privileges));
        if (role != null) {
            builder.add("role", role);
        }
        when(jwtTokenProvider.parseClaims("good-token")).thenReturn(builder.build());
    }

    private OrderResult sampleOrder() {
        return new OrderResult("order-1", "buyer-1", null, List.of(), new BigDecimal("150.00"),
                "AWAITING_PAYMENT", Instant.now(), null);
    }

    @Test
    void checkout_returns200AndUsesJwtSubjectAsBuyerId() throws Exception {
        authenticateAs("buyer-1", "CHECKOUT.START");
        when(startCheckoutUseCase.checkout("buyer-1")).thenReturn(sampleOrder());

        mockMvc.perform(post("/api/v1/checkout").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("order-1"));
    }

    @Test
    void checkout_returns409WhenCartIsEmpty() throws Exception {
        authenticateAs("buyer-1", "CHECKOUT.START");
        when(startCheckoutUseCase.checkout("buyer-1"))
                .thenThrow(new ConflictException("CART_EMPTY", "Cannot checkout an empty cart"));

        mockMvc.perform(post("/api/v1/checkout").header("Authorization", "Bearer good-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CART_EMPTY"));
    }

    @Test
    void listMyOrders_returns403WithoutPrivilege() throws Exception {
        authenticateAs("buyer-1");
        mockMvc.perform(get("/api/v1/orders/me").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrder_returns200() throws Exception {
        authenticateAs("buyer-1", "ORDER.VIEW");
        when(getOrderUseCase.getOrder("order-1")).thenReturn(sampleOrder());

        mockMvc.perform(get("/api/v1/orders/order-1").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AWAITING_PAYMENT"));
    }

    @Test
    void cancelOrder_returns200() throws Exception {
        authenticateAs("buyer-1", "ORDER.CANCEL");
        when(cancelOrderUseCase.cancel("order-1", "buyer-1")).thenReturn(
                new OrderResult("order-1", "buyer-1", null, List.of(), new BigDecimal("150.00"),
                        "CANCELLED", Instant.now(), null));

        mockMvc.perform(post("/api/v1/orders/order-1/cancel").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void listAllOrders_returns200ForAdmin() throws Exception {
        authenticateAsWithRole("admin-1", "ADMIN", "ORDER.LIST");
        when(listAllOrdersUseCase.list()).thenReturn(List.of(sampleOrder()));

        mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("order-1"));
    }

    @Test
    void listAllOrders_returns200ForSupportStaff() throws Exception {
        authenticateAsWithRole("support-1", "SUPPORT_STAFF", "ORDER.LIST");
        when(listAllOrdersUseCase.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());
    }

    @Test
    void listAllOrders_returns403ForBuyerDespiteHoldingThePrivilege() throws Exception {
        authenticateAsWithRole("buyer-1", "BUYER", "ORDER.LIST");

        mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ORDER_LIST_ALL_FORBIDDEN"));
    }

    @Test
    void listAllOrders_returns403WithoutThePrivilege() throws Exception {
        authenticateAsWithRole("admin-1", "ADMIN");

        mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());
    }
}
