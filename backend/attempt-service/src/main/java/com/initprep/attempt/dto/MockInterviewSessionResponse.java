package com.initprep.attempt.dto;

import com.initprep.attempt.enums.MockInterviewStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockInterviewSessionResponse {
    private UUID id;
    private String targetRole;
    private String difficulty;
    private Integer totalQuestions;
    private MockInterviewStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Double overallScore;
    private Integer attemptedQuestions;
    private Integer correctQuestions;
    private Double codingScore;
    private Double mcqScore;
    private Double theoryScore;
    private Integer codingAccepted;
    private Integer codingAttempted;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendedTopics;
    private List<MockInterviewQuestionResponse> questions;
}
