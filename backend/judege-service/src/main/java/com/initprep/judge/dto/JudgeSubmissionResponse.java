package com.initprep.judge.dto;

import com.initprep.judge.enums.JudgeStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeSubmissionResponse {

    private JudgeStatus status;

    private Integer passedTestCases;

    private Integer totalTestCases;

    private Long executionTime;

    private Long memoryUsed;

    private String compilerOutput;

    private String runtimeOutput;

    private List<TestCaseResult> testCaseResults;
}
