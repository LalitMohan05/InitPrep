package com.initprep.attempt.client;

import com.initprep.attempt.dto.QuestionDetailsResponse;
import com.initprep.attempt.dto.QuestionJudgeResponse;
import com.initprep.attempt.dto.McqAnswerResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.initprep.attempt.exception.InterviewServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InterviewServiceClient {

    private final RestClient restClient;
    private final HttpServletRequest request;

    @Value("${services.interview.url}")
    private String interviewServiceUrl;

    public boolean questionExists(UUID questionId) {

        try {

            String authorization =
                request.getHeader("Authorization");

            Boolean exists = restClient
                .get()
                .uri(
                    interviewServiceUrl +
                        "/api/questions/{questionId}/exists",
                    questionId
                )
                .header("Authorization", authorization)
                .retrieve()
                .body(Boolean.class);

            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {

            throw new InterviewServiceException(
                "Interview Service is unavailable",
                e
            );
        }
    }

    public QuestionJudgeResponse getJudgeData(UUID questionId) {

        try {

            String authorization =
                request.getHeader("Authorization");

            return restClient
                .get()
                .uri(
                    interviewServiceUrl +
                        "/api/questions/{questionId}/judge-data",
                    questionId
                )
                .header("Authorization", authorization)
                .retrieve()
                .body(QuestionJudgeResponse.class);

        } catch (Exception e) {

            throw new InterviewServiceException(
                "Failed to get judge data for question: " + questionId,
                e
            );
        }
    }

    public QuestionDetailsResponse getQuestionDetails(UUID questionId) {

        try {

            String authorization = request.getHeader("Authorization");

            return restClient
                .get()
                .uri(
                    interviewServiceUrl +
                        "/api/questions/{questionId}/details",
                    questionId
                )
                .header("Authorization", authorization)
                .retrieve()
                .body(QuestionDetailsResponse.class);

        } catch (Exception e) {

            throw new RuntimeException(
                "Failed to communicate with Interview Service",
                e
            );
        }
    }

    public List<UUID> findQuestionIds(String role, String difficulty, String type) {
        try {
            String authorization = request.getHeader("Authorization");
            StringBuilder uri = new StringBuilder(interviewServiceUrl)
                .append("/api/questions?role=").append(role)
                .append("&type=").append(type)
                .append("&page=0&size=1000&sort=title,asc");
            if (difficulty != null && !difficulty.isBlank()) uri.append("&difficulty=").append(difficulty);
            JsonNode page = restClient.get().uri(uri.toString())
                .header("Authorization", authorization).retrieve().body(JsonNode.class);
            if (page == null || !page.path("content").isArray()) return List.of();
            List<UUID> ids = new ArrayList<>();
            page.path("content").forEach(item -> {
                if (item.hasNonNull("id")) ids.add(UUID.fromString(item.path("id").asText()));
            });
            return ids;
        } catch (Exception e) {
            throw new InterviewServiceException("Failed to select questions for the mock interview", e);
        }
    }

    public boolean checkMcqAnswer(UUID questionId, String answer) {
        try {
            return Boolean.TRUE.equals(restClient.post()
                .uri(interviewServiceUrl + "/api/questions/{questionId}/check-answer", questionId)
                .header("Authorization", request.getHeader("Authorization"))
                .body(java.util.Map.of("answer", answer))
                .retrieve()
                .body(McqAnswerResponse.class)
                .isCorrect());
        } catch (Exception e) {
            throw new InterviewServiceException("Failed to evaluate MCQ answer", e);
        }
    }
}
