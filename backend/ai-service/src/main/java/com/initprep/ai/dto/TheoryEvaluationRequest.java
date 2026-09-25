package com.initprep.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TheoryEvaluationRequest {
    @NotBlank
    private String question;
    @NotBlank
    private String answer;
    private String targetRole;
    private String expectedAnswer;
    private String difficulty;
    private List<String> topics;
}
