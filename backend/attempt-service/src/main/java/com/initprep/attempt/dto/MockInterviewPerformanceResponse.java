package com.initprep.attempt.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MockInterviewPerformanceResponse {
    private String category;
    private Integer totalQuestions;
    private Integer attemptedQuestions;
    private Integer correctQuestions;
    private Double score;
}
