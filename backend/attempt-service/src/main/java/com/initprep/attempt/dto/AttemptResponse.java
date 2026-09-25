package com.initprep.attempt.dto;

import com.initprep.attempt.enums.AttemptResult;
import com.initprep.attempt.enums.AttemptStatus;
import com.initprep.attempt.enums.AttemptType;
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
    private AttemptType type;
    private String answer;
    private String language;

    private AttemptStatus status;

    private AttemptResult result;
    private Double score;

    private String feedback;
    private String compilerOutput;
    private String runtimeOutput;
    private TestCaseResult failedTestCase;
    private Integer passedTestCases;
    private Integer totalTestCases;
    private Long executionTime;
    private Long memoryUsed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
