package com.initprep.attempt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockInterviewAnswerRequest {
    @NotNull
    private UUID questionId;

    @NotBlank
    private String answer;

    private String language;
}
