package com.initprep.attempt;

import com.initprep.attempt.config.SecurityConfig;
import com.initprep.attempt.dto.JudgeSubmissionResponse;
import com.initprep.attempt.security.JwtService;
import com.initprep.attempt.service.interfaces.AttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(SecurityConfig.class)
class AttemptRunAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttemptService attemptService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void userWithValidJwtCanRunCode() throws Exception {
        UUID userId = UUID.fromString("69e3d947-8c8d-4fc4-a776-f03855597883");
        when(jwtService.isTokenValid("valid-user-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-user-token")).thenReturn(userId);
        when(jwtService.extractRole("valid-user-token")).thenReturn("USER");
        when(attemptService.runCode(eq(userId), any()))
            .thenReturn(JudgeSubmissionResponse.builder().status("ACCEPTED").build());

        mockMvc.perform(post("/api/attempts/run")
                .header("Authorization", "Bearer valid-user-token")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"questionId":"2ec38fe8-7c2d-4fd1-975a-2a963bfb3131","sourceCode":"class Main {}","language":"JAVA"}
                    """))
            .andExpect(status().isOk());

        verify(attemptService).runCode(eq(userId), any());
    }

    @Test
    void unauthenticatedRequestCannotRunCode() throws Exception {
        mockMvc.perform(post("/api/attempts/run")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"questionId":"2ec38fe8-7c2d-4fd1-975a-2a963bfb3131","sourceCode":"class Main {}","language":"JAVA"}
                    """))
            .andExpect(status().isForbidden());
    }
}
