package com.initprep.attempt;

import com.initprep.attempt.config.SecurityConfig;
import com.initprep.attempt.dto.JudgeSubmissionResponse;
import com.initprep.attempt.dto.AttemptResponse;
import com.initprep.attempt.dto.MockInterviewSessionResponse;
import com.initprep.attempt.security.JwtService;
import com.initprep.attempt.service.interfaces.AttemptService;
import com.initprep.attempt.service.interfaces.MockInterviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(SecurityConfig.class)
class AttemptRunAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttemptService attemptService;

    @MockitoBean
    private MockInterviewService mockInterviewService;

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

    @Test
    void mockInterviewHistoryRouteDoesNotMatchAttemptId() throws Exception {
        UUID userId = UUID.fromString("69e3d947-8c8d-4fc4-a776-f03855597883");
        when(jwtService.isTokenValid("valid-user-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-user-token")).thenReturn(userId);
        when(jwtService.extractRole("valid-user-token")).thenReturn("USER");
        when(mockInterviewService.history(eq(userId), any()))
            .thenReturn(new PageImpl<>(List.of(MockInterviewSessionResponse.builder().build())));

        mockMvc.perform(get("/api/attempts/mock-interviews?page=0&size=10&sort=startedAt,desc")
                .header("Authorization", "Bearer valid-user-token"))
            .andExpect(status().isOk());

        verify(mockInterviewService).history(eq(userId), any());
        verifyNoInteractions(attemptService);
    }

    @Test
    void validUuidRouteResolvesToAttemptDetails() throws Exception {
        UUID userId = UUID.fromString("69e3d947-8c8d-4fc4-a776-f03855597883");
        UUID attemptId = UUID.fromString("2ec38fe8-7c2d-4fd1-975a-2a963bfb3131");
        when(jwtService.isTokenValid("valid-user-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-user-token")).thenReturn(userId);
        when(jwtService.extractRole("valid-user-token")).thenReturn("USER");
        when(attemptService.getAttempt(userId, attemptId)).thenReturn(AttemptResponse.builder().id(attemptId).build());

        mockMvc.perform(get("/api/attempts/{attemptId}", attemptId)
                .header("Authorization", "Bearer valid-user-token"))
            .andExpect(status().isOk());

        verify(attemptService).getAttempt(userId, attemptId);
    }

    @Test
    void invalidAttemptIdReturnsNotFoundInsteadOfUuidConversionFailure() throws Exception {
        when(jwtService.isTokenValid("valid-user-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-user-token")).thenReturn(UUID.fromString("69e3d947-8c8d-4fc4-a776-f03855597883"));
        when(jwtService.extractRole("valid-user-token")).thenReturn("USER");

        mockMvc.perform(get("/api/attempts/{attemptId}", "not-a-uuid")
                .header("Authorization", "Bearer valid-user-token"))
            .andExpect(status().isNotFound());
    }
}
