package com.initprep.attempt.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheoryEvaluationResponse {
    private Double score;
    private String strengths;
    private String weaknesses;
    private String feedback;
    private List<String> recommendedTopics;
}
