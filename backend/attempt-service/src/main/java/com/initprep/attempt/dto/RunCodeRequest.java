package com.initprep.attempt.dto;

import com.initprep.attempt.enums.AttemptType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunCodeRequest {

    @NotNull
    private UUID questionId;

    @NotNull
    private AttemptType type;

    @NotBlank
    private String sourceCode;

    @NotBlank
    private String language;
}
