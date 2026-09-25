package com.initprep.ai.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TheoryEvaluationResponse {
    private Double score;
    private String strengths;
    private String weaknesses;
    private String feedback;
    private List<String> recommendedTopics;
}
