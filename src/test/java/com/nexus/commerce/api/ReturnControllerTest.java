package com.nexus.commerce.api;

import com.nexus.commerce.api.mapper.CommerceApiMapperImpl;
import com.nexus.commerce.application.usecase.CreateReturnRequestUseCase;
import com.nexus.commerce.application.usecase.ReceiveReturnUseCase;
import com.nexus.commerce.application.usecase.RejectReturnUseCase;
import com.nexus.commerce.application.usecase.ReturnRequestResult;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReturnController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, CommerceApiMapperImpl.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, PrivilegeAuthorizationAspect.class})
class ReturnControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CreateReturnRequestUseCase createReturnRequestUseCase;
    @MockBean private ReceiveReturnUseCase receiveReturnUseCase;
    @MockBean private RejectReturnUseCase rejectReturnUseCase;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    private void authenticateAs(String subject, String... privileges) {
        when(jwtTokenProvider.isValid("good-token")).thenReturn(true);
        Claims claims = Jwts.claims().subject(subject).add("privileges", List.of(privileges)).build();
        when(jwtTokenProvider.parseClaims("good-token")).thenReturn(claims);
    }

    private ReturnRequestResult sampleResult(String status) {
        return new ReturnRequestResult("return-1", "order-1", "Wrong item", status, Instant.now(), null, null);
    }

    @Test
    void createReturnRequest_returns200ForAdmin() throws Exception {
        authenticateAs("admin-1", "ORDER.REFUND");
        when(createReturnRequestUseCase.create("order-1", "Wrong item")).thenReturn(sampleResult("REQUESTED"));

        mockMvc.perform(post("/api/v1/orders/order-1/returns")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"reason":"Wrong item"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));
    }

    @Test
    void createReturnRequest_returns403WithoutPrivilege() throws Exception {
        authenticateAs("buyer-1", "ORDER.VIEW");

        mockMvc.perform(post("/api/v1/orders/order-1/returns")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"reason":"Wrong item"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReturnRequest_returns409WhenNotApplicable() throws Exception {
        authenticateAs("admin-1", "ORDER.REFUND");
        when(createReturnRequestUseCase.create("order-1", "Wrong item"))
                .thenThrow(new ConflictException("RETURN_NOT_APPLICABLE", "not applicable"));

        mockMvc.perform(post("/api/v1/orders/order-1/returns")
                        .header("Authorization", "Bearer good-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"reason":"Wrong item"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RETURN_NOT_APPLICABLE"));
    }

    @Test
    void receiveReturn_returns200ForAdmin() throws Exception {
        authenticateAs("admin-1", "ORDER.REFUND");
        when(receiveReturnUseCase.receive("return-1", "admin-1")).thenReturn(sampleResult("RECEIVED"));

        mockMvc.perform(post("/api/v1/returns/return-1/receive").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    void receiveReturn_returns403WithoutPrivilege() throws Exception {
        authenticateAs("buyer-1", "ORDER.VIEW");

        mockMvc.perform(post("/api/v1/returns/return-1/receive").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectReturn_returns200ForAdmin() throws Exception {
        authenticateAs("admin-1", "ORDER.REFUND");
        when(rejectReturnUseCase.reject("return-1", "admin-1")).thenReturn(sampleResult("REJECTED"));

        mockMvc.perform(post("/api/v1/returns/return-1/reject").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void rejectReturn_returns403WithoutPrivilege() throws Exception {
        authenticateAs("buyer-1", "ORDER.VIEW");

        mockMvc.perform(post("/api/v1/returns/return-1/reject").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());
    }
}
