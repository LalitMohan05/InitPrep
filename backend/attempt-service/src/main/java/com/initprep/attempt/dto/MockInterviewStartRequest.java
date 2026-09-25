package com.initprep.attempt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockInterviewStartRequest {
    @NotBlank
    private String targetRole;

    @NotBlank
    private String difficulty;

    @Min(5)
    @Max(15)
    private Integer totalQuestions;
}
