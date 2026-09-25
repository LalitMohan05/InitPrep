package com.initprep.attempt.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheoryEvaluationRequest {
    private String question;
    private String answer;
    private String targetRole;
}
