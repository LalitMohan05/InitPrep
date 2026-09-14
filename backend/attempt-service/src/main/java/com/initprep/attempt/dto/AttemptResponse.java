package com.initprep.attempt.dto;

import com.initprep.attempt.enums.AttemptResult;
import com.initprep.attempt.enums.AttemptStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptResponse {

    private UUID id;

    private UUID questionId;
    private String language;

    private AttemptStatus status;

    private AttemptResult result;
    private Double score;

    private String feedback;
    private String compilerOutput;
    private String runtimeOutput;
    private TestCaseResult failedTestCase;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
