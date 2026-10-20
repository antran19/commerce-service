package com.nexus.commerce.api;

import com.nexus.commerce.infrastructure.config.SecurityConfig;
import com.nexus.common.kafka.DeadLetterController;
import com.nexus.common.kafka.DeadLetterService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// common-kafka's operator endpoint as it is wired into this service: commerce's own SecurityConfig
// (not permitted, so no token = 401) plus the shared privilege check (SYSTEM.DLT.MANAGE = admin).
@WebMvcTest(DeadLetterController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class,
        PrivilegeAuthorizationAspect.class})
class DeadLetterControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DeadLetterService deadLetterService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    private void authenticateAs(String subject, String... privileges) {
        when(jwtTokenProvider.isValid("good-token")).thenReturn(true);
        Claims claims = Jwts.claims().subject(subject).add("privileges", List.of(privileges)).build();
        when(jwtTokenProvider.parseClaims("good-token")).thenReturn(claims);
    }

    @Test
    void status_withoutToken_is401() throws Exception {
        mockMvc.perform(get("/internal/dlt"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(deadLetterService);
    }

    @Test
    void replay_withoutPrivilege_is403() throws Exception {
        authenticateAs("seller-1", "PRODUCT.CREATE");

        mockMvc.perform(post("/internal/dlt/replay").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PRIVILEGE_DENIED"));
        verifyNoInteractions(deadLetterService);
    }

    @Test
    void status_asAdmin_returnsBacklog() throws Exception {
        authenticateAs("admin-1", DeadLetterController.PRIVILEGE);
        when(deadLetterService.status(5)).thenReturn(List.of(new DeadLetterService.TopicStatus(
                "fulfillment-events.commerce-service.DLT", "fulfillment-events", 2, List.of())));

        mockMvc.perform(get("/internal/dlt").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].backlog").value(2));
    }

    @Test
    void replay_asAdmin_reportsTheResult() throws Exception {
        authenticateAs("admin-1", DeadLetterController.PRIVILEGE);
        when(deadLetterService.replay(anyInt())).thenReturn(new DeadLetterService.ReplayResult(3, null));

        mockMvc.perform(post("/internal/dlt/replay").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replayed").value(3));
        verify(deadLetterService).replay(100);
    }
}
