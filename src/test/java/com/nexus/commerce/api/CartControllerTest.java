package com.nexus.commerce.api;

import com.nexus.commerce.api.mapper.CommerceApiMapperImpl;
import com.nexus.commerce.application.usecase.*;
import com.nexus.commerce.infrastructure.config.SecurityConfig;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, CommerceApiMapperImpl.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, PrivilegeAuthorizationAspect.class})
class CartControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AddCartItemUseCase addCartItemUseCase;
    @MockBean private UpdateCartItemUseCase updateCartItemUseCase;
    @MockBean private RemoveCartItemUseCase removeCartItemUseCase;
    @MockBean private GetCartUseCase getCartUseCase;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    private void authenticateAs(String subject, String... privileges) {
        when(jwtTokenProvider.isValid("good-token")).thenReturn(true);
        Claims claims = Jwts.claims().subject(subject).add("privileges", List.of(privileges)).build();
        when(jwtTokenProvider.parseClaims("good-token")).thenReturn(claims);
    }

    @Test
    void getMyCart_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/carts/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void getMyCart_returns403WithoutPrivilege() throws Exception {
        authenticateAs("buyer-1");
        mockMvc.perform(get("/api/v1/carts/me").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyCart_returns200WithCartForAuthenticatedBuyer() throws Exception {
        authenticateAs("buyer-1", "CART.VIEW");
        when(getCartUseCase.getCart("buyer-1"))
                .thenReturn(new CartResult("cart-1", "buyer-1", List.of(), BigDecimal.ZERO));

        mockMvc.perform(get("/api/v1/carts/me").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buyerId").value("buyer-1"));
    }

    @Test
    void addItem_returns200AndUsesJwtSubjectAsBuyerId() throws Exception {
        authenticateAs("buyer-1", "CART.CREATE");
        when(addCartItemUseCase.addItem("buyer-1", "product-1", 1)).thenReturn(laptopCart());

        mockMvc.perform(post("/api/v1/carts/items")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"productId":"product-1","quantity":1}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(999.00));
    }

    @Test
    void addItem_stillAcceptsTheOldBody_butIgnoresClientPriceNameAndSeller() throws Exception {
        authenticateAs("buyer-1", "CART.CREATE");
        when(addCartItemUseCase.addItem("buyer-1", "product-1", 1)).thenReturn(laptopCart());

        mockMvc.perform(post("/api/v1/carts/items")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"productId":"product-1","sellerId":"someone-else","productName":"Fake",
                                 "unitPrice":1,"quantity":1}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(999.00));
    }

    private static CartResult laptopCart() {
        return new CartResult("cart-1", "buyer-1",
                List.of(new CartItemResult("product-1", "seller-1", "Laptop X",
                        new BigDecimal("999.00"), 1, new BigDecimal("999.00"))),
                new BigDecimal("999.00"));
    }
}
